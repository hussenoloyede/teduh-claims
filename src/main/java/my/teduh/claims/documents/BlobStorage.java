package my.teduh.claims.documents;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

/**
 * The only class that talks to Blob Storage. Same pattern as ToolFleet: a private container, files that
 * are never overwritten, and short-lived read-only links instead of making anything public.
 */
@Component
@EnableConfigurationProperties(StorageProperties.class)
class BlobStorage {

	static final Duration LINK_LIFETIME = Duration.ofMinutes(15);

	private final BlobServiceClient service;

	private final BlobContainerClient container;

	/** A connection string carries the account key, which can sign links itself. Managed identity can't. */
	private final boolean signsWithAccountKey;

	BlobStorage(StorageProperties properties) {
		BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
		if (StringUtils.hasText(properties.connectionString())) {
			builder.connectionString(properties.connectionString());
			this.signsWithAccountKey = true;
		}
		else if (StringUtils.hasText(properties.endpoint())) {
			builder.endpoint(properties.endpoint()).credential(new DefaultAzureCredentialBuilder().build());
			this.signsWithAccountKey = false;
		}
		else {
			throw new IllegalStateException("Set teduh.storage.connection-string (Azurite) or "
					+ "teduh.storage.endpoint (Azure) so the app knows where to store documents.");
		}
		this.service = builder.buildClient();
		this.container = service.getBlobContainerClient(properties.container());
		this.container.createIfNotExists();
	}

	/** Uploads the bytes. "If-None-Match: *" makes Blob Storage refuse if a file already has this name. */
	void upload(String blobName, FileCheck.CheckedFile file) {
		BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromBytes(file.content()))
			.setHeaders(new BlobHttpHeaders().setContentType(file.type().contentType()))
			.setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));
		container.getBlobClient(blobName).uploadWithResponse(options, null, Context.NONE);
	}

	/**
	 * A SAS (shared access signature) link: the file's address plus a signed query string allowing one
	 * thing, reading this one file, until it expires. "attachment" makes the browser download the file
	 * rather than open it as a page on the storage domain.
	 *
	 * In Azure the link is signed with a user delegation key, which Azure issues to the managed identity for
	 * a limited time; no account key is involved. Azurite in tests uses its development key instead.
	 */
	DownloadLink link(String blobName, String fileName) {
		BlobClient blob = container.getBlobClient(blobName);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		OffsetDateTime expiry = now.plus(LINK_LIFETIME);
		BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry,
				new BlobSasPermission().setReadPermission(true))
			.setContentDisposition("attachment; filename=\"" + fileName + "\"");
		String signature = signsWithAccountKey ? blob.generateSas(values)
				: blob.generateUserDelegationSas(values, service.getUserDelegationKey(now.minusMinutes(5), expiry));
		return new DownloadLink(blob.getBlobUrl() + "?" + signature, expiry.toInstant());
	}

}
