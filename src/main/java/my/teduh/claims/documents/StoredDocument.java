package my.teduh.claims.documents;

import java.time.Instant;
import java.util.UUID;

/** A document as the rest of the app sees it: what it is and who added it, but not where it is stored. */
public record StoredDocument(UUID id, String fileName, String contentType, long sizeBytes, String sha256,
		UUID uploadedBy, Instant uploadedAt) {

}
