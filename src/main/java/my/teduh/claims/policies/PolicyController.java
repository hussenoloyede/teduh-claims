package my.teduh.claims.policies;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import my.teduh.claims.accounts.SignedInUser;

/** The signed-in customer's policies, for the home page and the first step of a new claim. */
@RestController
class PolicyController {

	private final PolicyLookup policies;

	PolicyController(PolicyLookup policies) {
		this.policies = policies;
	}

	@GetMapping("/api/policies")
	@PreAuthorize("hasRole('CUSTOMER')")
	public List<PolicyCover> mine(@AuthenticationPrincipal SignedInUser me) {
		return policies.policiesOf(me.id());
	}

}
