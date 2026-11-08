package my.teduh.claims.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The only way into the documents module from outside. It stores files for a claim, lists them and
 * hands out download links. It knows nothing about claim statuses or who may see a claim: the claims
 * module checks that before it asks.
 */
public interface DocumentStore {

	int MAX_FILES_PER_UPLOAD = 5;

	int MAX_FILES_PER_CLAIM = 15;

	/**
	 * Checks every file first, then stores them all. If any file breaks a rule, nothing is stored and a
	 * DocumentRejectedException says why. Joins the caller's transaction, so if the caller's work fails
	 * afterwards the document rows roll back with it.
	 */
	List<StoredDocument> store(UUID claimId, UUID uploadedBy, List<UploadedFile> files);

	/** A claim's documents in the order they were added. */
	List<StoredDocument> documentsOf(UUID claimId);

	/** Empty when the document doesn't exist or belongs to another claim. */
	Optional<DownloadLink> linkFor(UUID claimId, UUID documentId);

}
