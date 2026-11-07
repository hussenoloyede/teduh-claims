package my.teduh.claims.claims;

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

}
