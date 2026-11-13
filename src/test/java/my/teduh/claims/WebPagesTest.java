package my.teduh.claims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The addresses of the React app and the Storybook. Their files only exist inside the Docker image, so these
 * tests check the routing and the headers; docker-smoke checks the real pages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebPagesTest {

	@Autowired
	private MockMvcTester mvc;

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

}
