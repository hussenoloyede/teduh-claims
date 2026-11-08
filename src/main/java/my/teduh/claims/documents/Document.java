package my.teduh.claims.documents;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The record of one stored file. The bytes live in Blob Storage under blobName; this row describes them. */
@Entity
@Table(name = "documents")
class Document {

	@Id
	private UUID id;

	private UUID claimId;

	private String fileName;

	private String contentType;

	private long sizeBytes;

	private String sha256;

	private String blobName;

	private UUID uploadedBy;

	private Instant uploadedAt;

	protected Document() {
	}

	Document(UUID id, UUID claimId, FileCheck.CheckedFile file, String blobName, UUID uploadedBy, Instant uploadedAt) {
		this.id = id;
		this.claimId = claimId;
		this.fileName = file.name();
		this.contentType = file.type().contentType();
		this.sizeBytes = file.content().length;
		this.sha256 = file.sha256();
		this.blobName = blobName;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = uploadedAt;
	}

	String getFileName() {
		return fileName;
	}

	String getBlobName() {
		return blobName;
	}

	StoredDocument toStored() {
		return new StoredDocument(id, fileName, contentType, sizeBytes, sha256, uploadedBy, uploadedAt);
	}

}
