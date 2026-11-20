package my.teduh.claims.policies;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

/** The JSON for /api/plans and for buying a policy. */
public final class PlanApi {

	private PlanApi() {
	}

	/** One plan in the picker, with its demo price and the most Teduh pays per claim type. */
	public record PlanOffer(TravelPlan plan, String name, String blurb, BigDecimal pricePerTrip,
			Map<ClaimType, BigDecimal> limits) {

		static PlanOffer of(TravelPlan plan) {
			return new PlanOffer(plan, plan.displayName(), plan.blurb(), plan.pricePerTrip(), plan.limits());
		}

	}

	/** The plan and the trip: cover runs from the day the customer leaves to the day they get home. */
	public record BuyPolicyRequest(@NotNull TravelPlan plan, @NotNull LocalDate coverStart, @NotNull LocalDate coverEnd) {
	}

}
