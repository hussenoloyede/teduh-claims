package my.teduh.claims.claims;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import my.teduh.claims.claims.ClaimApi.ApproveRequest;
import my.teduh.claims.claims.ClaimApi.ClaimDetails;
import my.teduh.claims.claims.ClaimApi.ClaimSummary;
import my.teduh.claims.claims.ClaimApi.RejectRequest;
import my.teduh.claims.claims.ClaimApi.RequestInfoRequest;
import my.teduh.claims.claims.ClaimApi.RespondRequest;
import my.teduh.claims.claims.ClaimApi.StartReviewRequest;
import my.teduh.claims.claims.ClaimApi.SubmitClaimRequest;

/**
 * /api/claims. Thin on purpose: turn JSON into a service call and the result back into JSON.
 * Rules live in Claim and ClaimService; errors become HTTP answers in ClaimErrorHandler.
 *
 * WEEK 2 ONLY: who is acting comes from the X-Demo-User-Id header, and nothing checks roles or ownership.
 * Week 3 replaces the header with the signed-in user, adds customer and agent roles, and makes
 * someone else's claim answer 404.
 */
@RestController
@RequestMapping("/api/claims")
class ClaimController {

	static final String DEMO_USER = "X-Demo-User-Id";

	private final ClaimService service;

	ClaimController(ClaimService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ClaimDetails> submit(@RequestHeader(DEMO_USER) UUID userId,
			@Valid @RequestBody SubmitClaimRequest body) {
		Claim claim = service.submit(new NewClaim(userId, body.policyId(), body.claimType(), body.incidentDate(),
				body.amountClaimed(), body.description()));
		return ResponseEntity.created(URI.create("/api/claims/" + claim.getId())).body(details(claim));
	}

	/** Without ?tab: the user's own claims (customer home). With ?tab: that tab of the agent queue. */
	@GetMapping
	List<ClaimSummary> list(@RequestHeader(DEMO_USER) UUID userId, @RequestParam(required = false) String tab) {
		List<Claim> claims = (tab == null) ? service.claimsOf(userId)
				: service.queue(QueueTab.fromUrlName(tab)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown queue tab: " + tab)),
						userId);
		return claims.stream().map(ClaimSummary::of).toList();
	}

	@GetMapping("/{id}")
	ClaimDetails get(@PathVariable UUID id) {
		return details(service.find(id));
	}

	@PostMapping("/{id}/start-review")
	ClaimDetails startReview(@RequestHeader(DEMO_USER) UUID userId, @PathVariable UUID id,
			@Valid @RequestBody StartReviewRequest body) {
		return details(service.startReview(id, body.version(), userId));
	}

	@PostMapping("/{id}/request-info")
	ClaimDetails requestInfo(@RequestHeader(DEMO_USER) UUID userId, @PathVariable UUID id,
			@Valid @RequestBody RequestInfoRequest body) {
		return details(service.requestInfo(id, body.version(), userId, body.message()));
	}

	@PostMapping("/{id}/respond")
	ClaimDetails respond(@RequestHeader(DEMO_USER) UUID userId, @PathVariable UUID id,
			@Valid @RequestBody RespondRequest body) {
		return details(service.respond(id, body.version(), userId, body.note(), false));
	}

	@PostMapping("/{id}/approve")
	ClaimDetails approve(@RequestHeader(DEMO_USER) UUID userId, @PathVariable UUID id,
			@Valid @RequestBody ApproveRequest body) {
		return details(service.approve(id, body.version(), userId, body.amount(), body.reason()));
	}

	@PostMapping("/{id}/reject")
	ClaimDetails reject(@RequestHeader(DEMO_USER) UUID userId, @PathVariable UUID id,
			@Valid @RequestBody RejectRequest body) {
		return details(service.reject(id, body.version(), userId, body.reason()));
	}

	private ClaimDetails details(Claim claim) {
		return ClaimDetails.of(claim, service.timeline(claim.getId()));
	}

}
