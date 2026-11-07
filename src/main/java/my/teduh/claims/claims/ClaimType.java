package my.teduh.claims.claims;

/**
 * What went wrong on the trip. No medical claims: the MVP holds no health data, even made-up data.
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
