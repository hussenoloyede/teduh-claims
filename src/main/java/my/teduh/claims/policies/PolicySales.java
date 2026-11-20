package my.teduh.claims.policies;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import my.teduh.claims.shared.MalaysianTime;

/**
 * A customer choosing a plan for a trip (C16 to C19). Nothing is charged: Teduh is a demo. The policy is real
 * within the app, though, so the customer can claim on it from the day the cover starts.
 */
@Service
class PolicySales {

	/** The longest trip one policy covers. */
	static final int MAX_DAYS = 90;

	private final PolicyRepository policies;

	private final Clock clock;

	PolicySales(PolicyRepository policies, Clock clock) {
		this.policies = policies;
		this.clock = clock;
	}

	@Transactional
	public PolicyCover buy(UUID customerId, TravelPlan plan, LocalDate coverStart, LocalDate coverEnd) {
		LocalDate today = MalaysianTime.today(clock);
		if (coverStart.isBefore(today)) {
			throw new PolicyRuleException("Cover can't start before today.");
		}
		if (coverStart.isAfter(today.plusYears(1))) {
			throw new PolicyRuleException("Cover must start within a year from today.");
		}
		if (coverEnd.isBefore(coverStart)) {
			throw new PolicyRuleException("The trip can't end before it starts.");
		}
		// Both days count: a trip from 1 Dec to 14 Dec is 14 days of cover.
		if (ChronoUnit.DAYS.between(coverStart, coverEnd) + 1 > MAX_DAYS) {
			throw new PolicyRuleException("One policy covers at most " + MAX_DAYS + " days.");
		}
		boolean overlaps = policies.findByCustomerIdOrderByCoverStartDesc(customerId)
			.stream()
			.map(Policy::toCover)
			.anyMatch(other -> !coverStart.isAfter(other.coverEnd()) && !coverEnd.isBefore(other.coverStart()));
		if (overlaps) {
			throw new PolicyRuleException("You already have cover for some of those dates.");
		}
		// TTP-26-010004: the plan, the year it was bought, and a number from a database sequence, so two
		// policies bought at the same moment can't get the same number.
		String number = "%s-%02d-%06d".formatted(plan.prefix(), today.getYear() % 100, policies.nextPolicyNumber());
		Policy policy = new Policy(UUID.randomUUID(), number, customerId, plan.displayName(), coverStart, coverEnd,
				plan.limits());
		return policies.save(policy).toCover();
	}

}
