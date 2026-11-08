package my.teduh.claims.policies;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import my.teduh.claims.accounts.DemoAccounts;
import my.teduh.claims.shared.MalaysianTime;

/**
 * Gives the two demo customers a Teduh Travel Plus policy each, if they don't have one yet.
 * The cover runs from 40 days before today to 50 days after, so a recent trip is always inside it.
 * Order 2: runs after the demo accounts exist.
 *
 * The cover dates are set when the policy is first created. The daily demo reset (week 8) creates them
 * again, so the live demo never runs out of cover.
 */
@Component
@Order(2)
class DemoPolicySeeder implements ApplicationRunner {

	static final UUID AISYAHS_POLICY = UUID.fromString("7ed00000-0000-4000-8000-000000000101");

	static final UUID DANIELS_POLICY = UUID.fromString("7ed00000-0000-4000-8000-000000000102");

	/** The limits per claim type from the scope. */
	static final Map<ClaimType, BigDecimal> TRAVEL_PLUS_LIMITS = Map.of(ClaimType.FLIGHT_DELAY,
			new BigDecimal("500.00"), ClaimType.BAGGAGE_DELAYED, new BigDecimal("1000.00"),
			ClaimType.LOST_OR_DAMAGED_BAGGAGE, new BigDecimal("3000.00"), ClaimType.TRIP_CANCELLATION,
			new BigDecimal("10000.00"));

	private final PolicyRepository policies;

	private final Clock clock;

	DemoPolicySeeder(PolicyRepository policies, Clock clock) {
		this.policies = policies;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		LocalDate today = MalaysianTime.today(clock);
		List.of(travelPlus(AISYAHS_POLICY, "TTP-26-004817", DemoAccounts.AISYAH, today),
				travelPlus(DANIELS_POLICY, "TTP-26-005203", DemoAccounts.DANIEL, today))
			.stream()
			.filter(policy -> !policies.existsById(policy.getId()))
			.forEach(policies::save);
	}

	private static Policy travelPlus(UUID id, String number, UUID customer, LocalDate today) {
		return new Policy(id, number, customer, "Teduh Travel Plus", today.minusDays(40), today.plusDays(50),
				TRAVEL_PLUS_LIMITS);
	}

}
