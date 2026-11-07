package my.teduh.claims.claims;

/**
 * Builds the reference customers see, such as CLM-2026-000017, from the year and a number from the
 * claim_reference_seq sequence.
 */
final class ClaimReference {

	private ClaimReference() {
	}

	static String format(int year, long number) {
		if (number < 1 || number > 999_999) {
			throw new IllegalArgumentException("Claim numbers run from 1 to 999999, got " + number);
		}
		return "CLM-%d-%06d".formatted(year, number);
	}

}
