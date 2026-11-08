package my.teduh.claims.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentRepository extends JpaRepository<Document, UUID> {

	List<Document> findByClaimIdOrderByUploadedAtAscFileNameAsc(UUID claimId);

	long countByClaimId(UUID claimId);

	Optional<Document> findByIdAndClaimId(UUID id, UUID claimId);

}
