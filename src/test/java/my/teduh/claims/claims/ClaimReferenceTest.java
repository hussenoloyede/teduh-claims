package my.teduh.claims.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClaimReferenceTest {

	@Test
	void padsTheNumberToSixDigits() {
		assertThat(ClaimReference.format(2026, 17)).isEqualTo("CLM-2026-000017");
	}

	@Test
	void theLargestNumberStillFits() {
		assertThat(ClaimReference.format(2026, 999_999)).isEqualTo("CLM-2026-999999");
	}

	@Test
	void numbersOutsideTheRangeAreRefused() {
		assertThatThrownBy(() -> ClaimReference.format(2026, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ClaimReference.format(2026, 1_000_000)).isInstanceOf(IllegalArgumentException.class);
	}

}
