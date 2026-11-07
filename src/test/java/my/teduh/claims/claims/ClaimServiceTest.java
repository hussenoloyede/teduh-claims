package my.teduh.claims.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.policies.ClaimType;

/**
 * The claims service against real PostgreSQL, with the clock fixed at 7 Nov 2026, 9:14 pm Malaysian time.
 * Each test makes its own customer and policy, so tests never see each other's data.
 */
@SpringBootTest
@Import({ TestcontainersConfiguration.class, ClaimServiceTest.FixedClock.class })
class ClaimServiceTest {

	@TestConfiguration
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-07T13:14:00Z"), ZoneOffset.UTC);
		}

	}

	private static final UUID FARID = UUID.fromString("00000000-0000-0000-0000-00000000000f");
	private static final UUID MEI_LING = UUID.fromString("00000000-0000-0000-0000-00000000000e");
	private static final String ASK = "Please upload the airline's delay report showing your bag arrived on 16 Oct.";

	@Autowired
	private ClaimService service;

	@Autowired
	private JdbcTemplate jdbc;

	private final UUID aisyah = UUID.randomUUID();

	/** Teduh Travel Plus for one customer: cover 1 Oct to 31 Dec 2026, with the four limits from the scope. */
	private UUID travelPlusFor(UUID customer) {
		UUID policy = UUID.randomUUID();
		jdbc.update("""
				insert into policies (id, policy_number, customer_id, plan_name, cover_start, cover_end)
				values (?, ?, ?, 'Teduh Travel Plus', date '2026-10-01', date '2026-12-31')""",
				policy, "TT-" + policy.toString().substring(0, 8), customer);
		String limit = "insert into policy_limits (policy_id, claim_type, limit_amount) values (?, ?, ?)";
		jdbc.update(limit, policy, "FLIGHT_DELAY", new BigDecimal("500.00"));
		jdbc.update(limit, policy, "BAGGAGE_DELAYED", new BigDecimal("1000.00"));
		jdbc.update(limit, policy, "LOST_OR_DAMAGED_BAGGAGE", new BigDecimal("3000.00"));
		jdbc.update(limit, policy, "TRIP_CANCELLATION", new BigDecimal("10000.00"));
		return policy;
	}

	private NewClaim baggageDelayed(UUID policy, LocalDate incident, String amount) {
		return new NewClaim(aisyah, policy, ClaimType.BAGGAGE_DELAYED, incident, new BigDecimal(amount),
				"My checked luggage arrived 48 hours late in Kuala Lumpur.");
	}

	@Test
	void aSubmittedClaimGetsAReferenceAndTheFirstLineOfItsTimeline() {
		UUID policy = travelPlusFor(aisyah);

		Claim claim = service.submit(baggageDelayed(policy, LocalDate.of(2026, 10, 14), "450.00"));

		assertThat(claim.getReference()).matches("CLM-2026-\\d{6}");
		assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
		assertThat(claim.getSubmittedAt()).isEqualTo(Instant.parse("2026-11-07T13:14:00Z"));
		assertThat(claim.getVersion()).isZero();
		assertThat(service.timeline(claim.getId())).singleElement()
			.satisfies(event -> {
				assertThat(event.getToStatus()).isEqualTo(ClaimStatus.SUBMITTED);
				assertThat(event.getActorId()).isEqualTo(aisyah);
			});
	}

	@Test
	void someoneElsesPolicyLooksLikeNoPolicy() {
		UUID danielsPolicy = travelPlusFor(UUID.randomUUID());

		assertThatThrownBy(() -> service.submit(baggageDelayed(danielsPolicy, LocalDate.of(2026, 10, 14), "450.00")))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("We couldn't find that policy on your account.");
	}

	@Test
	void theIncidentMustBeInsideTheCoverPeriod() {
		UUID policy = travelPlusFor(aisyah);

		assertThatThrownBy(() -> service.submit(baggageDelayed(policy, LocalDate.of(2026, 9, 30), "450.00")))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("The incident date must be between 1 Oct 2026 and 31 Dec 2026, when your policy was active.");
	}

	@Test
	void theIncidentCannotBeInTheFuture() {
		UUID policy = travelPlusFor(aisyah);

		// 8 Nov is inside the cover but after "today" (7 Nov in Malaysia).
		assertThatThrownBy(() -> service.submit(baggageDelayed(policy, LocalDate.of(2026, 11, 8), "450.00")))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("The incident date can't be in the future.");
	}

	@Test
	void todayCountsAsInThePast() {
		UUID policy = travelPlusFor(aisyah);

		Claim claim = service.submit(baggageDelayed(policy, LocalDate.of(2026, 11, 7), "450.00"));

		assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
	}

	@Test
	void theAmountCannotExceedTheLimitForThatClaimType() {
		UUID policy = travelPlusFor(aisyah);

		assertThatThrownBy(() -> service.submit(baggageDelayed(policy, LocalDate.of(2026, 10, 14), "1000.01")))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("Your limit for Baggage delayed is RM 1,000.00.");
	}

	@Test
	void theDemoStoryFromSubmitToApproval() {
		UUID policy = travelPlusFor(aisyah);

		Claim claim = service.submit(baggageDelayed(policy, LocalDate.of(2026, 10, 14), "450.00"));
		claim = service.startReview(claim.getId(), claim.getVersion(), FARID);
		claim = service.requestInfo(claim.getId(), claim.getVersion(), FARID, ASK);
		claim = service.respond(claim.getId(), claim.getVersion(), aisyah, "Here is the delay report.", true);
		claim = service.approve(claim.getId(), claim.getVersion(), FARID, new BigDecimal("400.00"),
				"Receipts total RM 400.");

		assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
		assertThat(claim.getVersion()).isEqualTo(4L);
		assertThat(service.find(claim.getId()).getApprovedAmount()).isEqualByComparingTo("400.00");
		assertThat(service.timeline(claim.getId())).extracting(ClaimEvent::getToStatus)
			.containsExactly(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.MORE_INFO_NEEDED,
					ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED);
	}

	@Test
	void anActionBasedOnAnOldVersionIsRefusedBeforeAnyRuleIsChecked() {
		UUID policy = travelPlusFor(aisyah);
		Claim claim = service.submit(baggageDelayed(policy, LocalDate.of(2026, 10, 14), "450.00"));
		long bothSaw = claim.getVersion();

		service.startReview(claim.getId(), bothSaw, FARID);

		assertThatThrownBy(() -> service.startReview(claim.getId(), bothSaw, MEI_LING))
			.isInstanceOf(OptimisticLockingFailureException.class)
			.hasMessage("This claim changed while you were looking at it.");
		assertThat(service.find(claim.getId()).getAssignedAgentId()).isEqualTo(FARID);
		assertThat(service.timeline(claim.getId())).hasSize(2);
	}

	@Test
	void aRefusedActionLeavesNoTraceInTheTimeline() {
		UUID policy = travelPlusFor(aisyah);
		Claim claim = service.submit(baggageDelayed(policy, LocalDate.of(2026, 10, 14), "450.00"));
		Claim underReview = service.startReview(claim.getId(), claim.getVersion(), FARID);

		assertThatThrownBy(() -> service.approve(underReview.getId(), underReview.getVersion(), MEI_LING,
				new BigDecimal("450.00"), null))
			.isInstanceOf(ClaimRuleException.class);
		assertThat(service.find(claim.getId()).getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
		assertThat(service.timeline(claim.getId())).hasSize(2);
	}

	@Test
	void anUnknownClaimIsNotFound() {
		UUID nobody = UUID.randomUUID();

		assertThatThrownBy(() -> service.startReview(nobody, 0, FARID)).isInstanceOf(ClaimNotFoundException.class);
	}

}
