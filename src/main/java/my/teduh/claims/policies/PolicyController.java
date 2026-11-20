package my.teduh.claims.policies;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import my.teduh.claims.accounts.SignedInUser;
import my.teduh.claims.policies.PlanApi.BuyPolicyRequest;
import my.teduh.claims.policies.PlanApi.PlanOffer;

/**
 * The signed-in customer's policies, for the home page and the first step of a new claim, and the plans a
 * customer can choose from when they have no cover yet.
 */
@RestController
class PolicyController {

	private final PolicyLookup policies;

	private final PolicySales sales;

	PolicyController(PolicyLookup policies, PolicySales sales) {
		this.policies = policies;
		this.sales = sales;
	}

	@GetMapping("/api/policies")
	@PreAuthorize("hasRole('CUSTOMER')")
	public List<PolicyCover> mine(@AuthenticationPrincipal SignedInUser me) {
		return policies.policiesOf(me.id());
	}

	/** The three plans, cheapest first, with demo prices. */
	@GetMapping("/api/plans")
	@PreAuthorize("hasRole('CUSTOMER')")
	public List<PlanOffer> plans() {
		return Arrays.stream(TravelPlan.values()).map(PlanOffer::of).toList();
	}

	/** Buys a plan for a trip. Nothing is charged; the policy works in the app straight away. */
	@PostMapping("/api/policies")
	@PreAuthorize("hasRole('CUSTOMER')")
	@ApiResponse(responseCode = "201", description = "The new policy")
	public ResponseEntity<PolicyCover> buy(@AuthenticationPrincipal SignedInUser me,
			@Valid @RequestBody BuyPolicyRequest body) {
		PolicyCover policy = sales.buy(me.id(), body.plan(), body.coverStart(), body.coverEnd());
		return ResponseEntity.created(URI.create("/api/policies/" + policy.policyId())).body(policy);
	}

}
