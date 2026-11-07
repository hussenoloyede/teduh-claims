package my.teduh.claims.claims;

import static my.teduh.claims.claims.ClaimStatus.APPROVED;
import static my.teduh.claims.claims.ClaimStatus.MORE_INFO_NEEDED;
import static my.teduh.claims.claims.ClaimStatus.REJECTED;
import static my.teduh.claims.claims.ClaimStatus.SUBMITTED;
import static my.teduh.claims.claims.ClaimStatus.UNDER_REVIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaimTest {

	private static final UUID AISYAH = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final UUID DANIEL = UUID.fromString("00000000-0000-0000-0000-00000000000d");
	private static final UUID FARID = UUID.fromString("00000000-0000-0000-0000-00000000000f");
	private static final UUID MEI_LING = UUID.fromString("00000000-0000-0000-0000-00000000000e");

	private static final UUID TRAVEL_PLUS = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private static final String REFERENCE = "CLM-2026-000017";
	private static final Instant SUBMITTED_AT = Instant.parse("2026-11-07T13:14:00Z");
	private static final String WHAT_HAPPENED = "My checked luggage arrived 48 hours late in Kuala Lumpur.";

	private static final String ASK = "Please upload the airline's delay report showing your bag arrived on 16 Oct.";

	static NewClaim aisyahsDetails(String amount, String description) {
		return new NewClaim(AISYAH, TRAVEL_PLUS, ClaimType.BAGGAGE_DELAYED, LocalDate.of(2026, 10, 14), rm(amount),
				description);
	}

	private static Claim submitted() {
		return Claim.submit(REFERENCE, aisyahsDetails("450.00", WHAT_HAPPENED), SUBMITTED_AT);
	}

	private static Claim underReviewByFarid() {
		Claim claim = submitted();
		claim.startReview(FARID);
		return claim;
	}

	private static Claim waitingOnAisyah() {
		Claim claim = underReviewByFarid();
		claim.requestInfo(FARID, ASK);
		return claim;
	}

	private static BigDecimal rm(String amount) {
		return new BigDecimal(amount);
	}

	@Nested
	class Submitting {

		@Test
		void aNewClaimIsSubmittedWithNoAgent() {
			Claim claim = submitted();

			assertThat(claim.getStatus()).isEqualTo(SUBMITTED);
			assertThat(claim.getAssignedAgentId()).isNull();
			assertThat(claim.getCustomerId()).isEqualTo(AISYAH);
			assertThat(claim.getReference()).isEqualTo(REFERENCE);
			assertThat(claim.getClaimType()).isEqualTo(ClaimType.BAGGAGE_DELAYED);
			assertThat(claim.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
			assertThat(claim.getVersion()).isNull();
		}

		@ParameterizedTest
		@ValueSource(strings = { "0", "0.00", "-10.00" })
		void theAmountClaimedMustBeMoreThanZero(String amount) {
			assertThatThrownBy(() -> Claim.submit(REFERENCE, aisyahsDetails(amount, WHAT_HAPPENED), SUBMITTED_AT))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("The amount claimed must be more than zero.");
		}

		@Test
		void amountsHaveAtMostTwoDecimalPlaces() {
			assertThatThrownBy(() -> Claim.submit(REFERENCE, aisyahsDetails("450.005", WHAT_HAPPENED), SUBMITTED_AT))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Amounts can have at most two decimal places.");
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { "  " })
		void theCustomerMustSayWhatHappened(String description) {
			assertThatThrownBy(() -> Claim.submit(REFERENCE, aisyahsDetails("450.00", description), SUBMITTED_AT))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Tell us what happened.");
		}

		@Test
		void theDescriptionCanBeAtMostFiveHundredCharacters() {
			assertThatThrownBy(() -> Claim.submit(REFERENCE, aisyahsDetails("450.00", "a".repeat(501)), SUBMITTED_AT))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("The description can be at most 500 characters.");
		}

	}

	@Nested
	class StartingAReview {

		@Test
		void assignsTheClaimToThatAgent() {
			Claim claim = submitted();

			ClaimTransition transition = claim.startReview(FARID);

			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
			assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
			assertThat(transition).isEqualTo(new ClaimTransition(SUBMITTED, UNDER_REVIEW, FARID, null));
		}

		@Test
		void aSecondAgentCannotStartAReviewThatIsAlreadyRunning() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.startReview(MEI_LING))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Can't start a review on a claim that is Under review.");
			assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
		}

	}

	@Nested
	class RequestingInfo {

		@Test
		void theAssignedAgentCanAskTheCustomerForSomething() {
			Claim claim = underReviewByFarid();

			ClaimTransition transition = claim.requestInfo(FARID, ASK);

			assertThat(claim.getStatus()).isEqualTo(MORE_INFO_NEEDED);
			assertThat(transition).isEqualTo(new ClaimTransition(UNDER_REVIEW, MORE_INFO_NEEDED, FARID, ASK));
		}

		@Test
		void anotherAgentCannotAsk() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.requestInfo(MEI_LING, ASK))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Only the agent assigned to this claim can do that.");
			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
		}

		@Test
		void cannotAskBeforeTheReviewHasStarted() {
			Claim claim = submitted();

			assertThatThrownBy(() -> claim.requestInfo(FARID, ASK))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Can't ask for more information on a claim that is Submitted.");
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { "   " })
		void theMessageIsRequired(String message) {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.requestInfo(FARID, message))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("A message to the customer is required.");
		}

		@Test
		void theMessageCanBeUpToOneThousandCharacters() {
			Claim claim = underReviewByFarid();

			claim.requestInfo(FARID, "a".repeat(1_000));

			assertThat(claim.getStatus()).isEqualTo(MORE_INFO_NEEDED);
		}

		@Test
		void aLongerMessageIsRefused() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.requestInfo(FARID, "a".repeat(1_001)))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("The message can be at most 1000 characters.");
		}

	}

	@Nested
	class Responding {

		@Test
		void theCustomerRespondsWithANoteAndTheClaimGoesBackToTheSameAgent() {
			Claim claim = waitingOnAisyah();

			ClaimTransition transition = claim.respond(AISYAH, "  Here is the report.  ", true);

			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
			assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
			assertThat(transition).isEqualTo(new ClaimTransition(MORE_INFO_NEEDED, UNDER_REVIEW, AISYAH, "Here is the report."));
		}

		@Test
		void filesWithoutANoteAreEnough() {
			Claim claim = waitingOnAisyah();

			ClaimTransition transition = claim.respond(AISYAH, null, true);

			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
			assertThat(transition.message()).isNull();
		}

		@Test
		void aResponseNeedsANoteOrAFile() {
			Claim claim = waitingOnAisyah();

			assertThatThrownBy(() -> claim.respond(AISYAH, " ", false))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Add a note, a file, or both.");
			assertThat(claim.getStatus()).isEqualTo(MORE_INFO_NEEDED);
		}

		@Test
		void anotherCustomerCannotRespond() {
			Claim claim = waitingOnAisyah();

			assertThatThrownBy(() -> claim.respond(DANIEL, "Not my claim", false))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Only the customer who made this claim can respond.");
		}

		@Test
		void theCustomerCannotRespondWhenNothingWasAsked() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.respond(AISYAH, "Extra note", false))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Can't respond on a claim that is Under review.");
		}

		@Test
		void theNoteCanBeAtMostFiveHundredCharacters() {
			Claim claim = waitingOnAisyah();

			assertThatThrownBy(() -> claim.respond(AISYAH, "a".repeat(501), false))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("The note can be at most 500 characters.");
		}

		@Test
		void thereCanBeSeveralRounds() {
			Claim claim = waitingOnAisyah();
			claim.respond(AISYAH, "Here is the report.", true);
			claim.requestInfo(FARID, "The report is cut off. Please upload all pages.");
			claim.respond(AISYAH, null, true);

			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
			assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
		}

	}

	@Nested
	class Approving {

		@Test
		void theFullAmountNeedsNoReason() {
			Claim claim = underReviewByFarid();

			ClaimTransition transition = claim.approve(FARID, rm("450.00"), null);

			assertThat(claim.getStatus()).isEqualTo(APPROVED);
			assertThat(claim.getApprovedAmount()).isEqualByComparingTo("450");
			assertThat(claim.getDecisionReason()).isNull();
			assertThat(transition).isEqualTo(new ClaimTransition(UNDER_REVIEW, APPROVED, FARID, null));
		}

		@Test
		void lessThanClaimedNeedsAReason() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.approve(FARID, rm("400.00"), " "))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("A reason is required when approving less than the amount claimed.");
			assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
		}

		@Test
		void lessThanClaimedWithAReasonIsApproved() {
			Claim claim = underReviewByFarid();

			claim.approve(FARID, rm("400.00"), "Receipts total RM 400.");

			assertThat(claim.getStatus()).isEqualTo(APPROVED);
			assertThat(claim.getApprovedAmount()).isEqualByComparingTo("400.00");
			assertThat(claim.getDecisionReason()).isEqualTo("Receipts total RM 400.");
		}

		@Test
		void cannotApproveMoreThanClaimed() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.approve(FARID, rm("450.01"), null))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("You can't approve more than the RM 450.00 claimed.");
		}

		@ParameterizedTest
		@ValueSource(strings = { "0", "-1.00" })
		void theApprovedAmountMustBeMoreThanZero(String amount) {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.approve(FARID, rm(amount), "Nothing covered"))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("The approved amount must be more than zero.");
		}

		@Test
		void anotherAgentCannotApprove() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.approve(MEI_LING, rm("450.00"), null))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Only the agent assigned to this claim can do that.");
		}

		@Test
		void cannotApproveWhileWaitingOnTheCustomer() {
			Claim claim = waitingOnAisyah();

			assertThatThrownBy(() -> claim.approve(FARID, rm("450.00"), null))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Can't approve on a claim that is More info needed.");
		}

	}

	@Nested
	class Rejecting {

		@Test
		void aRejectionKeepsTheReasonForTheCustomer() {
			Claim claim = underReviewByFarid();
			String reason = "The bag was delayed by 3 hours. Your policy covers delays of 6 hours or more.";

			ClaimTransition transition = claim.reject(FARID, reason);

			assertThat(claim.getStatus()).isEqualTo(REJECTED);
			assertThat(claim.getDecisionReason()).isEqualTo(reason);
			assertThat(claim.getApprovedAmount()).isNull();
			assertThat(transition).isEqualTo(new ClaimTransition(UNDER_REVIEW, REJECTED, FARID, reason));
		}

		@ParameterizedTest
		@NullAndEmptySource
		void aReasonIsAlwaysRequired(String reason) {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.reject(FARID, reason))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("A reason is required to reject a claim.");
		}

		@Test
		void anotherAgentCannotReject() {
			Claim claim = underReviewByFarid();

			assertThatThrownBy(() -> claim.reject(MEI_LING, "Not covered"))
				.isInstanceOf(ClaimRuleException.class)
				.hasMessage("Only the agent assigned to this claim can do that.");
		}

	}

	@Nested
	class AfterADecision {

		@Test
		void nothingElseCanHappenToAnApprovedClaim() {
			Claim claim = underReviewByFarid();
			claim.approve(FARID, rm("450.00"), null);

			assertThat(claim.getStatus().isFinal()).isTrue();
			assertThatThrownBy(() -> claim.reject(FARID, "Changed my mind")).isInstanceOf(ClaimRuleException.class);
			assertThatThrownBy(() -> claim.requestInfo(FARID, ASK)).isInstanceOf(ClaimRuleException.class);
			assertThatThrownBy(() -> claim.startReview(MEI_LING)).isInstanceOf(ClaimRuleException.class);
			assertThat(claim.getStatus()).isEqualTo(APPROVED);
		}

		@Test
		void nothingElseCanHappenToARejectedClaim() {
			Claim claim = underReviewByFarid();
			claim.reject(FARID, "Not covered");

			assertThat(claim.getStatus().isFinal()).isTrue();
			assertThatThrownBy(() -> claim.approve(FARID, rm("450.00"), null)).isInstanceOf(ClaimRuleException.class);
			assertThat(claim.getStatus()).isEqualTo(REJECTED);
		}

	}

}
