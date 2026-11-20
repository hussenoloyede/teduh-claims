package my.teduh.claims.policies;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PolicyRepository extends JpaRepository<Policy, UUID> {

	Optional<Policy> findByIdAndCustomerId(UUID id, UUID customerId);

	List<Policy> findByCustomerIdOrderByCoverStartDesc(UUID customerId);

	/** The next number for a bought policy, from policy_number_seq (V8). */
	@Query(value = "select nextval('policy_number_seq')", nativeQuery = true)
	long nextPolicyNumber();

}
