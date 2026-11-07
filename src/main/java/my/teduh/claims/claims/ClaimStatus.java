package my.teduh.claims.claims;

/**
 * The five places a claim can be. APPROVED and REJECTED are final.
 */
public enum ClaimStatus {

	SUBMITTED("Submitted"),
	UNDER_REVIEW("Under review"),
	MORE_INFO_NEEDED("More info needed"),
	APPROVED("Approved"),
	REJECTED("Rejected");

	private final String label;

	ClaimStatus(String label) {
		this.label = label;
	}

	/** The wording people see on screen, for example "Under review". */
	public String label() {
		return label;
	}

	public boolean isFinal() {
		return this == APPROVED || this == REJECTED;
	}

}
