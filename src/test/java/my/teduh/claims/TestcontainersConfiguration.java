package my.teduh.claims;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The real services the app talks to, each in Docker for the length of the test run: PostgreSQL 18 and
 * Azurite, Microsoft's local stand-in for Blob Storage.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * Azurite's built-in development account. Microsoft publishes this key in its documentation; it only
	 * opens Azurite on this machine, never a real storage account.
	 */
	private static final String AZURITE_ACCOUNT = "devstoreaccount1";

	private static final String AZURITE_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

	private static final int AZURITE_BLOB_PORT = 10000;

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	/**
	 * Only the Blob service. --skipApiVersionCheck lets a newer Azure SDK talk to this Azurite version;
	 * --loose makes Azurite ignore request headers it doesn't support instead of refusing the request.
	 */
	@Bean
	GenericContainer<?> azurite() {
		return new GenericContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0"))
			.withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--skipApiVersionCheck", "--loose")
			.withExposedPorts(AZURITE_BLOB_PORT);
	}

	/**
	 * Spring Boot has no @ServiceConnection for Azurite, so this hands the app its address in the setting
	 * the app already reads, teduh.storage.connection-string. The port is random, chosen by Docker.
	 */
	@Bean
	DynamicPropertyRegistrar azuriteConnection(@Qualifier("azurite") GenericContainer<?> azurite) {
		return registry -> registry.add("teduh.storage.connection-string",
				() -> "DefaultEndpointsProtocol=http;AccountName=" + AZURITE_ACCOUNT + ";AccountKey=" + AZURITE_KEY
						+ ";BlobEndpoint=http://" + azurite.getHost() + ":" + azurite.getMappedPort(AZURITE_BLOB_PORT)
						+ "/" + AZURITE_ACCOUNT + ";");
	}

}
