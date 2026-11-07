package my.teduh.claims.policies;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Answers PolicyLookup questions. Package-private: other modules depend on the interface, not this class. */
@Service
@Transactional(readOnly = true)
class PolicyService implements PolicyLookup {

	private final PolicyRepository policies;

	PolicyService(PolicyRepository policies) {
		this.policies = policies;
	}

	@Override
	public Optional<PolicyCover> findForCustomer(UUID policyId, UUID customerId) {
		return policies.findByIdAndCustomerId(policyId, customerId).map(Policy::toCover);
	}

	@Override
	public List<PolicyCover> policiesOf(UUID customerId) {
		return policies.findByCustomerIdOrderByCoverStartDesc(customerId).stream().map(Policy::toCover).toList();
	}

}
