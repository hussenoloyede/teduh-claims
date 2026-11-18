package my.teduh.claims.accounts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.PostgreSqlJdbcIndexedSessionRepositoryCustomizer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.util.StringUtils;

/**
 * Sign-in sessions live in PostgreSQL (Spring Session JDBC, set up by Spring Boot from the starter in pom.xml),
 * so a session survives the app scaling to zero and starting again, and a second replica could share them.
 */
@Configuration(proxyBeanMethods = false)
class SessionStore {

	/**
	 * PostgreSQL's own "insert, or do nothing if it is already there" for session attributes. The React app
	 * sends several requests at once (who am I, the bell, the page's data), and two of them saving the same
	 * attribute at the same moment would otherwise fail with a duplicate key.
	 */
	@Bean
	PostgreSqlJdbcIndexedSessionRepositoryCustomizer postgreSqlSessionQueries() {
		return new PostgreSqlJdbcIndexedSessionRepositoryCustomizer();
	}

	/**
	 * The SESSION cookie, written from the server.servlet.session.cookie settings in application.yaml.
	 * Without this, Spring Session copies the web server's own cookie settings, which a real Tomcat has but the
	 * tests' stand-in server doesn't: there the cookie went out with no HttpOnly, Secure or SameSite at all.
	 * Being explicit means the tests check exactly what the browser gets.
	 */
	@Bean
	DefaultCookieSerializer sessionCookie(@Value("${server.servlet.session.cookie.http-only:true}") boolean httpOnly,
			@Value("${server.servlet.session.cookie.secure:true}") boolean secure,
			@Value("${server.servlet.session.cookie.same-site:lax}") String sameSite) {
		DefaultCookieSerializer cookie = new DefaultCookieSerializer();
		cookie.setCookieName("SESSION");
		cookie.setCookiePath("/");
		cookie.setUseHttpOnlyCookie(httpOnly);
		cookie.setUseSecureCookie(secure);
		cookie.setSameSite(StringUtils.capitalize(sameSite));
		return cookie;
	}

}
