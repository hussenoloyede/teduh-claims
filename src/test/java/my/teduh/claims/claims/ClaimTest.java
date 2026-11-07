package my.teduh.claims.claims;

import static my.teduh.claims.claims.ClaimStatus.SUBMITTED;
import static my.teduh.claims.claims.ClaimStatus.UNDER_REVIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClaimTest {

	private static final UUID AISYAH = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final UUID FARID = UUID.fromString("00000000-0000-0000-0000-00000000000f");
	private static final UUID MEI_LING = UUID.fromString("00000000-0000-0000-0000-00000000000e");

	private static Claim aisyahsClaim() {
		return Claim.submit(AISYAH, new BigDecimal("450.00"));
	}

	@Test
	void aNewClaimIsSubmittedWithNoAgent() {
		Claim claim = aisyahsClaim();

		assertThat(claim.getStatus()).isEqualTo(SUBMITTED);
		assertThat(claim.getAssignedAgentId()).isNull();
		assertThat(claim.getCustomerId()).isEqualTo(AISYAH);
	}

	@Test
	void startingAReviewAssignsTheClaimToThatAgent() {
		Claim claim = aisyahsClaim();

		ClaimTransition transition = claim.startReview(FARID);

		assertThat(claim.getStatus()).isEqualTo(UNDER_REVIEW);
		assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
		assertThat(transition).isEqualTo(new ClaimTransition(SUBMITTED, UNDER_REVIEW, FARID, null));
	}

	@Test
	void aSecondAgentCannotStartAReviewThatIsAlreadyRunning() {
		Claim claim = aisyahsClaim();
		claim.startReview(FARID);

		assertThatThrownBy(() -> claim.startReview(MEI_LING))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("Can't start a review on a claim that is Under review.");
		assertThat(claim.getAssignedAgentId()).isEqualTo(FARID);
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-10.00" })
	void theAmountClaimedMustBeMoreThanZero(String amount) {
		assertThatThrownBy(() -> Claim.submit(AISYAH, new BigDecimal(amount)))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("The amount claimed must be more than zero.");
	}

	@Test
	void amountsHaveAtMostTwoDecimalPlaces() {
		assertThatThrownBy(() -> Claim.submit(AISYAH, new BigDecimal("450.005")))
			.isInstanceOf(ClaimRuleException.class)
			.hasMessage("Amounts can have at most two decimal places.");
	}

}
