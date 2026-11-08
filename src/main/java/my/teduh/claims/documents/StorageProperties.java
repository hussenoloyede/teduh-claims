package my.teduh.claims.documents;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where documents are stored, from the teduh.storage.* settings. Set exactly one of:
 * - connectionString: Azurite, locally and in tests. It holds Azurite's well-known development key.
 * - endpoint: the real storage account in Azure, such as https://teduhhussein.blob.core.windows.net,
 *   reached with the app's managed identity. No key or password anywhere.
 */
@ConfigurationProperties("teduh.storage")
record StorageProperties(String connectionString, String endpoint, @DefaultValue("claim-documents") String container) {

}
