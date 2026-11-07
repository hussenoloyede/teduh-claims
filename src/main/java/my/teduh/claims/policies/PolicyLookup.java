package my.teduh.claims.policies;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The only way into the policies module from outside. The claims module asks it questions;
 * it never touches the policy tables itself.
 */
public interface PolicyLookup {

	/** The policy, but only if it belongs to this customer. Someone else's policy looks the same as no policy. */
	Optional<PolicyCover> findForCustomer(UUID policyId, UUID customerId);

	/** A customer's policies, newest cover first. */
	List<PolicyCover> policiesOf(UUID customerId);

}
