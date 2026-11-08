package my.teduh.claims.claims;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data writes the code for this interface at startup: save, findById and the rest come from
 * JpaRepository. Package-private, so only the claims module can reach its own table.
 */
interface ClaimRepository extends JpaRepository<Claim, UUID> {

	/** The next number for a claim reference. Each call returns a new number, even across transactions. */
	@Query(value = "select nextval('claim_reference_seq')", nativeQuery = true)
	long nextReferenceNumber();

	/** The claim, but only if it is this customer's. Someone else's claim comes back empty, like a made-up id. */
	Optional<Claim> findByIdAndCustomerId(UUID id, UUID customerId);

	/** A customer's own claims, newest first, for their home page. */
	List<Claim> findByCustomerIdOrderBySubmittedAtDesc(UUID customerId);

	/** The agent queue's "Submitted" tab: anyone's unassigned claims, oldest first. */
	List<Claim> findByStatusOrderBySubmittedAtAsc(ClaimStatus status);

	/** The other queue tabs: one agent's claims in the given statuses, oldest first. */
	List<Claim> findByStatusInAndAssignedAgentIdOrderBySubmittedAtAsc(Collection<ClaimStatus> statuses, UUID agentId);

}
