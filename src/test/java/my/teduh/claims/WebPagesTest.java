package my.teduh.claims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The addresses of the React app and the Storybook. The real files only exist inside the Docker image, so
 * these tests use small stand-ins from src/test/resources/static (index.html and assets/app.js);
 * docker-smoke checks the real pages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebPagesTest {

	private static final String REACT_PAGE = "Test copy of the React page";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void rootShowsTheReactApp() {
		assertThat(mvc.get().uri("/").exchange()).hasForwardedUrl("/index.html");
	}

	@Test
	void reactAppAddressesShowTheReactAppWithoutSigningIn() {
		for (String address : new String[] { "/sign-in", "/claims/0b7e6f2a-1c3d-4e5f-8a9b-0c1d2e3f4a5b",
				"/agent/queue" }) {
			assertThat(mvc.get().uri(address).exchange()).as(address)
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
				.bodyText()
				.contains(REACT_PAGE);
		}
	}

	@Test
	void realFilesAreServedAsThemselves() {
		assertThat(mvc.get().uri("/assets/app.js").exchange()).hasStatusOk()
			.bodyText()
			.contains("Test copy of a built script");
	}

	@Test
	void missingFilesAreNotFoundRatherThanTheReactApp() {
		assertThat(mvc.get().uri("/assets/missing.js").exchange()).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.get().uri("/storybook/missing").exchange()).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void apiAddressesNeverGetTheReactApp() {
		// Signed out, an unknown API address is refused by the security filters before it gets anywhere.
		assertThat(mvc.get().uri("/api/no-such-thing").exchange()).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void storybookWithoutSlashRedirectsToTheFolder() {
		assertThat(mvc.get().uri("/storybook").exchange()).hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/storybook/");
	}

	@Test
	void storybookFolderShowsItsIndexWithoutSigningIn() {
		assertThat(mvc.get().uri("/storybook/").exchange()).hasForwardedUrl("/storybook/index.html");
	}

	@Test
	void pagesMayBeFramedByTeduhOnly() {
		assertThat(mvc.get().uri("/actuator/health/liveness").exchange()).hasHeader("X-Frame-Options", "SAMEORIGIN");
	}

	@Test
	void onlyAddressesWithoutAFileNameBelongToTheReactApp() {
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("claims/123")).isTrue();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("claims/")).isTrue();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("profile")).isTrue();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("assets/index-abc123.js")).isFalse();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("favicon.svg")).isFalse();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("api/claims")).isFalse();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("storybook/missing")).isFalse();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("v3/api-docs")).isFalse();
		assertThat(WebPages.ReactAppResolver.isReactAppAddress("actuator/env")).isFalse();
	}

}
