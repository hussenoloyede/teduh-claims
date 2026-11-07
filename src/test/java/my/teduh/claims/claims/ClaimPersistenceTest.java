package my.teduh.claims.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import my.teduh.claims.TestcontainersConfiguration;

import my.teduh.claims.policies.ClaimType;

/**
 * Saves claims to a real PostgreSQL 18 in Docker, through the tables Flyway created.
 * Each step runs in its own transaction, the way separate web requests will.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ClaimPersistenceTest {

	private static final UUID FARID = UUID.fromString("00000000-0000-0000-0000-00000000000f");
	private static final UUID MEI_LING = UUID.fromString("00000000-0000-0000-0000-00000000000e");
	private static final Instant SUBMITTED_AT = Instant.parse("2026-11-07T13:14:00Z");

	@Autowired
	private ClaimRepository claims;

	@Autowired
	private ClaimEventRepository events;

	@Autowired
	private TransactionTemplate transaction;

	@Autowired
	private JdbcTemplate jdbc;

	private Claim newClaim() {
		String reference = ClaimReference.format(2026, claims.nextReferenceNumber());
		return Claim.submit(reference, ClaimTest.aisyahsDetails("450.00", "My checked luggage arrived 48 hours late."),
				SUBMITTED_AT);
	}

	@Test
	void aClaimIsSavedAndLoadedWithEveryField() {
		Claim claim = newClaim();
		transaction.executeWithoutResult(status -> claims.save(claim));

		Claim loaded = transaction.execute(status -> claims.findById(claim.getId()).orElseThrow());

		assertThat(loaded.getReference()).isEqualTo(claim.getReference());
		assertThat(loaded.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
		assertThat(loaded.getClaimType()).isEqualTo(ClaimType.BAGGAGE_DELAYED);
		assertThat(loaded.getIncidentDate()).isEqualTo(claim.getIncidentDate());
		assertThat(loaded.getAmountClaimed()).isEqualByComparingTo("450.00");
		assertThat(loaded.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
		assertThat(loaded.getVersion()).isZero();
	}

	@Test
	void eachReferenceNumberIsUsedOnlyOnce() {
		long first = claims.nextReferenceNumber();
		long second = claims.nextReferenceNumber();

		assertThat(second).isGreaterThan(first);
	}

	@Test
	void twoClaimsCannotShareAReference() {
		Claim first = newClaim();
		transaction.executeWithoutResult(status -> claims.save(first));
		Claim copy = Claim.submit(first.getReference(),
				ClaimTest.aisyahsDetails("100.00", "Same reference on purpose."), SUBMITTED_AT);

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> claims.saveAndFlush(copy)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aSaveBasedOnAnOutOfDateCopyIsRefused() {
		Claim claim = newClaim();
		transaction.executeWithoutResult(status -> claims.save(claim));

		// Farid and Mei Ling both open the same submitted claim: two copies, both at version 0.
		Claim faridsCopy = transaction.execute(status -> claims.findById(claim.getId()).orElseThrow());
		Claim meiLingsCopy = transaction.execute(status -> claims.findById(claim.getId()).orElseThrow());

		// Farid starts the review first. The row moves to version 1.
		faridsCopy.startReview(FARID);
		transaction.executeWithoutResult(status -> claims.save(faridsCopy));

		// Mei Ling's copy still says version 0 and still looks SUBMITTED to her, so the claim rules allow it.
		// The database is what catches it: "update ... where id = ? and version = 0" matches no row.
		meiLingsCopy.startReview(MEI_LING);
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> claims.save(meiLingsCopy)))
			.isInstanceOf(OptimisticLockingFailureException.class);

		Claim stored = transaction.execute(status -> claims.findById(claim.getId()).orElseThrow());
		assertThat(stored.getAssignedAgentId()).isEqualTo(FARID);
		assertThat(stored.getVersion()).isEqualTo(1L);
	}

	@Test
	void theTimelineComesBackInOrder() {
		Claim claim = newClaim();
		transaction.executeWithoutResult(status -> {
			claims.save(claim);
			events.save(ClaimEvent.submitted(claim));
		});
		transaction.executeWithoutResult(status -> {
			Claim loaded = claims.findById(claim.getId()).orElseThrow();
			ClaimTransition started = loaded.startReview(FARID);
			events.save(ClaimEvent.of(loaded.getId(), started, Instant.parse("2026-11-08T01:00:00Z")));
		});

		List<ClaimEvent> timeline = events.findByClaimIdOrderByOccurredAtAscIdAsc(claim.getId());

		assertThat(timeline).extracting(ClaimEvent::getToStatus)
			.containsExactly(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW);
		assertThat(timeline.get(1).getActorId()).isEqualTo(FARID);
	}

	@Test
	void theDatabaseItselfRefusesAnApprovalAboveTheAmountClaimed() {
		// A second line of defence: even if some code skipped Claim.approve, the table's check constraint holds.
		Claim claim = newClaim();
		transaction.executeWithoutResult(status -> claims.save(claim));

		assertThatThrownBy(() -> jdbc.update("update claims set approved_amount = 450.01 where id = ?", claim.getId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
