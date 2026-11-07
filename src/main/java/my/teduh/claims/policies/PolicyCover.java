package my.teduh.claims.policies;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What other modules may know about a policy: whose it is, when it covers, and the limit per claim type.
 * A read-only copy, so nothing outside the policies module can change a policy.
 */
public record PolicyCover(UUID policyId, UUID customerId, String policyNumber, String planName, LocalDate coverStart,
		LocalDate coverEnd, Map<ClaimType, BigDecimal> limits) {

	public PolicyCover {
		limits = Map.copyOf(limits);
	}

	/** True when the date falls inside the cover period, both ends included. */
	public boolean covers(LocalDate date) {
		return !date.isBefore(coverStart) && !date.isAfter(coverEnd);
	}

	/** Empty when the policy doesn't cover this type of claim at all. */
	public Optional<BigDecimal> limitFor(ClaimType type) {
		return Optional.ofNullable(limits.get(type));
	}

}
