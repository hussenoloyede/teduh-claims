package my.teduh.claims.accounts;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Who may call what, and how signing in and out works. Spring Security is a chain of servlet filters that
 * runs before any controller; this class configures that chain.
 *
 * Sign-in: the React app posts the email and password as a form to /api/auth/login. Spring Security checks
 * them, starts a session and sends back the session cookie. The cookie is HttpOnly (JavaScript can't read
 * it), so an injected script can't steal the sign-in. Settings for the cookie are in application.yaml.
 *
 * Roles are checked on each controller method with @PreAuthorize, which @EnableMethodSecurity switches on.
 * The rules below only decide what needs a sign-in at all.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(requests -> requests
			.requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
			.requestMatchers("/actuator/health/**").permitAll()
			.requestMatchers("/actuator/**").denyAll()
			.requestMatchers("/api/**").authenticated()
			// Everything else is the React app's files (from week 6), which anyone may load.
			.anyRequest().permitAll())

			// CSRF: because the browser sends the session cookie automatically, another website could make a
			// signed-in person's browser post to Teduh. The token lives in an XSRF-TOKEN cookie that only
			// Teduh's own pages can read (so not HttpOnly); they copy it into an X-XSRF-TOKEN header on every
			// change. A forged request from another site can't read the cookie, so it can't send the header.
			// spa() makes Spring Security accept that plain header value. The cookie storage is spelled out too,
			// so it's clear from this file where the token lives.
			.csrf(csrf -> {
				csrf.spa();
				csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
			})

			.formLogin(form -> form
				// The React sign-in page. Setting it also stops Spring Security generating its own login page.
				.loginPage("/sign-in")
				.loginProcessingUrl("/api/auth/login")
				.usernameParameter("email")
				.passwordParameter("password")
				// 204 instead of a redirect: the React app decides where to go next.
				.successHandler((request, response, authentication) -> response.setStatus(204))
				// The same message for a wrong email and a wrong password, so it doesn't reveal which accounts exist.
				.failureHandler((request, response, exception) -> problem(response, 401, "Sign-in failed",
						"That email and password don't match.")))

			// Spring Security forbids every page from being shown in a frame (X-Frame-Options: DENY), which stops
			// other sites hiding Teduh inside theirs to trick clicks. The Storybook shows each story in a frame
			// on its own page, so frames from Teduh itself are allowed. Other sites are still refused.
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.deleteCookies("JSESSIONID")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))

			// Problem details instead of redirects to a login page, which an API caller can't use.
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint(
						(request, response, exception) -> problem(response, 401, "Not signed in", "Please sign in."))
				.accessDeniedHandler((request, response, exception) -> problem(response, 403, "Not allowed",
						"You don't have access to that.")));
		return http.build();
	}

	/**
	 * Writes the same RFC 9457 shape as the controllers' errors. These run in the filter chain, before
	 * Spring MVC, so the @RestControllerAdvice handlers can't produce them. The text is fixed, never from
	 * the request, so it needs no escaping.
	 */
	private static void problem(HttpServletResponse response, int status, String title, String detail)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter()
			.write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\""
					+ detail + "\"}");
	}

}
