package my.teduh.claims.policies;

/**
 * What went wrong on the trip. It lives in the policies module because a policy decides which types it
 * covers and up to how much; the claims module uses it from here.
 * No medical claims: the MVP holds no health data, even made-up data.
 */
public enum ClaimType {

	FLIGHT_DELAY("Flight delay"),
	BAGGAGE_DELAYED("Baggage delayed"),
	LOST_OR_DAMAGED_BAGGAGE("Lost or damaged baggage"),
	TRIP_CANCELLATION("Trip cancellation");

	private final String label;

	ClaimType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
