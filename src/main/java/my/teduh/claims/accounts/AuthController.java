package my.teduh.claims.accounts;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The two sign-in endpoints that aren't handled by Spring Security's filters. Signing in and out
 * (/api/auth/login and /logout) are handled by the filters themselves; see SecurityConfiguration.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

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

	/** Never includes the password hash. */
	public record Me(UUID id, String email, String displayName, Role role) {
	}

}
