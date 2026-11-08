package my.teduh.claims.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.BrowserCsrf;
import my.teduh.claims.TestcontainersConfiguration;

/** Signing in and out with the seeded demo accounts, the way the React app will. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SignInTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void withoutSigningInTheApiAnswers401() {
		MvcTestResult result = mvc.get().uri("/api/auth/me").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(read(result, "$.title")).isEqualTo("Not signed in");
		assertThat(read(result, "$.detail")).isEqualTo("Please sign in.");
	}

	@Test
	void signingInStartsASessionThatKnowsWhoYouAre() {
		MockHttpSession session = signIn("aisyah@example.com", DemoAccounts.PASSWORD);

		MvcTestResult me = mvc.get().uri("/api/auth/me").session(session).exchange();

		assertThat(me).hasStatus(HttpStatus.OK);
		assertThat(read(me, "$.id")).isEqualTo(DemoAccounts.AISYAH.toString());
		assertThat(read(me, "$.displayName")).isEqualTo("Aisyah Rahman");
		assertThat(read(me, "$.role")).isEqualTo("CUSTOMER");
		assertThat(body(me)).doesNotContain("password").doesNotContain("bcrypt");
	}

	@Test
	void theBrowserGetsATokenCookieAndSendsItBackAsAHeader() {
		// 1. On load, the React app asks for a token. It arrives as a cookie that JavaScript can read.
		MvcTestResult first = mvc.get().uri("/api/auth/csrf").exchange();
		assertThat(first).hasStatus(HttpStatus.NO_CONTENT);
		Cookie token = first.getResponse().getCookie("XSRF-TOKEN");
		assertThat(token).isNotNull();
		assertThat(token.isHttpOnly()).isFalse();

		// 2. The browser sends the cookie back by itself; the app copies its value into the header.
		MvcTestResult signIn = mvc.post()
			.uri("/api/auth/login")
			.cookie(token)
			.header("X-XSRF-TOKEN", token.getValue())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", "farid@example.com")
			.param("password", DemoAccounts.PASSWORD)
			.exchange();

		assertThat(signIn).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void aForgedRequestWithoutTheTokenIsRefused() {
		MvcTestResult result = mvc.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", "farid@example.com")
			.param("password", DemoAccounts.PASSWORD)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() {
		MvcTestResult wrongPassword = attemptSignIn("aisyah@example.com", "not-the-password");
		MvcTestResult unknownEmail = attemptSignIn("nobody@example.com", DemoAccounts.PASSWORD);

		assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(unknownEmail).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(read(wrongPassword, "$.detail")).isEqualTo("That email and password don't match.");
		assertThat(body(unknownEmail)).isEqualTo(body(wrongPassword));
	}

	@Test
	void theEmailCanBeTypedWithCapitals() {
		assertThat(attemptSignIn("Farid@Example.com", DemoAccounts.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void signingOutEndsTheSession() {
		MockHttpSession session = signIn("farid@example.com", DemoAccounts.PASSWORD);

		// Signing in cleared the old token, so the app fetches a fresh one first.
		MvcTestResult signOut = mvc.post().uri("/api/auth/logout").session(session).with(BrowserCsrf.token(mvc)).exchange();

		assertThat(signOut).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/auth/me").session(session).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	// Helpers

	private MockHttpSession signIn(String email, String password) {
		MvcTestResult result = attemptSignIn(email, password);
		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MvcTestResult attemptSignIn(String email, String password) {
		return mvc.post()
			.uri("/api/auth/login")
			.with(BrowserCsrf.token(mvc))
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", email)
			.param("password", password)
			.exchange();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		}
		catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object read(MvcTestResult result, String path) {
		return JsonPath.read(body(result), path);
	}

}
