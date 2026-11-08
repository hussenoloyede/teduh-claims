package my.teduh.claims;

import jakarta.servlet.http.Cookie;

import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Adds a CSRF token to a test request exactly the way the React app will: ask /api/auth/csrf for the
 * XSRF-TOKEN cookie, then send the cookie back with its value copied into the X-XSRF-TOKEN header.
 *
 * Why not Spring Security's own csrf() test helper: it swaps the app's token store for a session-based
 * test one, and the swap outlives the test that made it. Every later test sharing the same running app
 * would then be testing the test store instead of the real cookie. With this helper, every test that
 * changes something also proves the real token flow works.
 */
public final class BrowserCsrf {

	private BrowserCsrf() {
	}

	public static RequestPostProcessor token(MockMvcTester mvc) {
		Cookie cookie = mvc.get().uri("/api/auth/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
		if (cookie == null) {
			throw new IllegalStateException("/api/auth/csrf sent no XSRF-TOKEN cookie");
		}
		return request -> {
			request.setCookies(cookie);
			request.addHeader("X-XSRF-TOKEN", cookie.getValue());
			return request;
		};
	}

}
