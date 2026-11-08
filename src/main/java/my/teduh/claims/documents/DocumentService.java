package my.teduh.claims.documents;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores files in Blob Storage first and records them in the database second, inside the caller's
 * transaction. If the upload fails, the transaction rolls back and no row points at a missing file. If the
 * database fails after the upload, the worst case is an unused file in storage, which is harmless.
 */
@Service
@Transactional
class DocumentService implements DocumentStore {

	private final DocumentRepository documents;

	private final BlobStorage storage;

	private final Clock clock;

	DocumentService(DocumentRepository documents, BlobStorage storage, Clock clock) {
		this.documents = documents;
		this.storage = storage;
		this.clock = clock;
	}

	@Override
	public List<StoredDocument> store(UUID claimId, UUID uploadedBy, List<UploadedFile> files) {
		if (files.size() > MAX_FILES_PER_UPLOAD) {
			throw new DocumentRejectedException("Attach up to " + MAX_FILES_PER_UPLOAD + " files at a time.");
		}
		if (documents.countByClaimId(claimId) + files.size() > MAX_FILES_PER_CLAIM) {
			throw new DocumentRejectedException("A claim can have at most " + MAX_FILES_PER_CLAIM + " documents.");
		}
		// Every file is checked before any is stored, so one bad file means none are kept.
		List<FileCheck.CheckedFile> checked = files.stream().map(FileCheck::check).toList();

		// One time for the whole upload, so files sent together sort by name rather than by microseconds.
		Instant now = clock.instant();
		return checked.stream().map(file -> {
			UUID id = UUID.randomUUID();
			String blobName = "claims/" + claimId + "/" + id + "-" + file.name();
			storage.upload(blobName, file);
			return documents.save(new Document(id, claimId, file, blobName, uploadedBy, now)).toStored();
		}).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredDocument> documentsOf(UUID claimId) {
		return documents.findByClaimIdOrderByUploadedAtAscFileNameAsc(claimId)
			.stream()
			.map(Document::toStored)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DownloadLink> linkFor(UUID claimId, UUID documentId) {
		return documents.findByIdAndClaimId(documentId, claimId)
			.map(document -> storage.link(document.getBlobName(), document.getFileName()));
	}

}
