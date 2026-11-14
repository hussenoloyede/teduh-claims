package my.teduh.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The OpenAPI document, and the saved copy of it in web/openapi.json that the React app's types come from.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsTest {

	/** Relative to the repository root, where Maven runs the tests. */
	private static final Path SAVED_COPY = Path.of("web", "openapi.json");

	@Autowired
	private MockMvcTester mvc;

	/**
	 * If an endpoint or a record changed, this rewrites web/openapi.json and fails, so the change can't slip
	 * through unnoticed. Then: npm run api:types in web/, fix whatever TypeScript now complains about, and
	 * commit both files. Line endings are ignored, because Windows and the Linux pipeline write them differently.
	 */
	@Test
	void savedCopyForTheReactAppMatchesTheApi() throws IOException {
		MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
		assertThat(result).hasStatusOk();

		String current = result.getResponse().getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n");
		if (!current.endsWith("\n")) {
			current += "\n";
		}
		String saved = Files.exists(SAVED_COPY) ? Files.readString(SAVED_COPY).replace("\r\n", "\n") : "";

		if (!current.equals(saved)) {
			Files.writeString(SAVED_COPY, current);
			fail("web/openapi.json didn't match the API and has been rewritten. Run npm run api:types in web/, "
					+ "check the React app still builds, and commit both files.");
		}
	}

	@Test
	void fieldsAreRequiredUnlessMarkedNullable() {
		assertThat(mvc.get().uri("/v3/api-docs").exchange()).bodyJson()
			.extractingPath("$.components.schemas.ClaimDetails.required")
			.asArray()
			.contains("id", "reference", "status", "version", "documents", "timeline")
			.doesNotContain("assignedAgentId", "approvedAmount", "decisionReason");
	}

	@Test
	void submittingAClaimIsDocumentedAs201WithTheClaim() {
		var docs = assertThat(mvc.get().uri("/v3/api-docs").exchange()).bodyJson();
		docs.extractingPath("$.paths['/api/claims'].post.responses['201'].content['application/json'].schema['$ref']")
			.isEqualTo("#/components/schemas/ClaimDetails");
		docs.doesNotHavePath("$.paths['/api/claims'].post.responses['200']");
	}

	@Test
	void signedInUserAndCsrfTokenAreNotParameters() {
		var docs = assertThat(mvc.get().uri("/v3/api-docs").exchange()).bodyJson();
		docs.extractingPath("$.paths['/api/claims/{id}'].get.parameters[*].name").asArray().containsExactly("id");
		docs.doesNotHavePath("$.paths['/api/auth/csrf'].get.parameters");
	}

}
