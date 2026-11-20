package my.teduh.claims.accounts;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The sign-in endpoints that aren't handled by Spring Security's filters. Signing in and out with a password
 * (/api/auth/login and /logout) are handled by the filters themselves; see SecurityConfiguration.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final NewDemoCustomers newCustomers;

	/** Where Spring Security keeps the signed-in person between requests: the session (in PostgreSQL). */
	private final SecurityContextRepository sessionStore = new HttpSessionSecurityContextRepository();

	AuthController(NewDemoCustomers newCustomers) {
		this.newCustomers = newCustomers;
	}

	/** Who is signed in. The React app calls this on load to decide between the customer and agent screens. */
	@GetMapping("/me")
	public Me me(@AuthenticationPrincipal SignedInUser user) {
		return new Me(user.id(), user.email(), user.displayName(), user.role());
	}

	/**
	 * Hands the browser a fresh XSRF-TOKEN cookie. Spring Security only creates the token when something
	 * reads it, and clears it after sign-in and sign-out, so the React app calls this on load and after
	 * each of those. Reading the token here is what makes Spring Security set the cookie.
	 */
	@GetMapping("/csrf")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	// The token comes from Spring Security, not from the caller, so the API document leaves it out.
	public void csrf(@Parameter(hidden = true) CsrfToken token) {
		token.getToken();
	}

	/**
	 * "Try as a new customer": creates a fresh demo customer and signs them in, the way a sign-in would.
	 * The session gets a new id (so a session id someone planted before can't be reused) and the CSRF
	 * token is cleared, like after a normal sign-in, so the React app fetches a fresh one.
	 */
	@PostMapping("/new-customer")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void newCustomer(HttpServletRequest request, HttpServletResponse response) {
		SignedInUser user = newCustomers.create();
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
		SecurityContextHolder.setContext(context);
		sessionStore.saveContext(context, request, response);
		CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null, request, response);
	}

	/** Never includes the password hash. */
	public record Me(UUID id, String email, String displayName, Role role) {
	}

}
