package my.teduh.claims.claims;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClaimEventRepository extends JpaRepository<ClaimEvent, Long> {

	/** A claim's timeline, oldest first. Spring Data builds the query from the method name. */
	List<ClaimEvent> findByClaimIdOrderByOccurredAtAscIdAsc(UUID claimId);

}
