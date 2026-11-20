package my.teduh.claims.policies;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The three Teduh travel plans a new customer can choose from (C16). Each covers the same four claim types;
 * higher plans pay more per claim. The price is a demo price: Teduh is a portfolio project and charges nothing.
 */
public enum TravelPlan {

	ESSENTIAL("TTE", "Teduh Travel Essential", "For short, simple trips", 29, 300, 500, 1_500, 5_000),
	PLUS("TTP", "Teduh Travel Plus", "Good cover for most trips", 49, 500, 1_000, 3_000, 10_000),
	MAX("TTM", "Teduh Travel Max", "Highest limits, for long or costly trips", 89, 1_000, 2_000, 6_000, 20_000);

	/** The start of each policy number: TTP-26-010004 is a Teduh Travel Plus policy. */
	private final String prefix;

	private final String displayName;

	private final String blurb;

	private final BigDecimal pricePerTrip;

	private final Map<ClaimType, BigDecimal> limits;

	TravelPlan(String prefix, String displayName, String blurb, int price, int flightDelay, int baggageDelayed,
			int lostOrDamaged, int tripCancellation) {
		this.prefix = prefix;
		this.displayName = displayName;
		this.blurb = blurb;
		this.pricePerTrip = money(price);
		this.limits = Map.of(ClaimType.FLIGHT_DELAY, money(flightDelay), ClaimType.BAGGAGE_DELAYED,
				money(baggageDelayed), ClaimType.LOST_OR_DAMAGED_BAGGAGE, money(lostOrDamaged),
				ClaimType.TRIP_CANCELLATION, money(tripCancellation));
	}

	private static BigDecimal money(int ringgit) {
		return BigDecimal.valueOf(ringgit).setScale(2);
	}

	public String prefix() {
		return prefix;
	}

	public String displayName() {
		return displayName;
	}

	public String blurb() {
		return blurb;
	}

	public BigDecimal pricePerTrip() {
		return pricePerTrip;
	}

	public Map<ClaimType, BigDecimal> limits() {
		return limits;
	}

}
