package my.teduh.claims.policies;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyRepository extends JpaRepository<Policy, UUID> {

	Optional<Policy> findByIdAndCustomerId(UUID id, UUID customerId);

	List<Policy> findByCustomerIdOrderByCoverStartDesc(UUID customerId);

}
