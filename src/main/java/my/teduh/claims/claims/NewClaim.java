package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import my.teduh.claims.policies.ClaimType;

/**
 * What the customer sends when submitting a claim. The reference number and the time come from the system.
 * Checks that need the policy (cover dates, limit per claim type) happen in the claims service,
 * which can ask the policies module. Checks the claim can make on its own happen in Claim.submit.
 */
public record NewClaim(UUID customerId, UUID policyId, ClaimType claimType, LocalDate incidentDate,
		BigDecimal amountClaimed, String description) {
}
