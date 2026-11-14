package my.teduh.claims.claims;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import my.teduh.claims.accounts.SignedInUser;
import my.teduh.claims.claims.ClaimApi.ApproveRequest;
import my.teduh.claims.claims.ClaimApi.ClaimDetails;
import my.teduh.claims.claims.ClaimApi.ClaimSummary;
import my.teduh.claims.claims.ClaimApi.RejectRequest;
import my.teduh.claims.claims.ClaimApi.RequestInfoRequest;
import my.teduh.claims.claims.ClaimApi.RespondRequest;
import my.teduh.claims.claims.ClaimApi.StartReviewRequest;
import my.teduh.claims.claims.ClaimApi.SubmitClaimRequest;
import my.teduh.claims.documents.DownloadLink;
import my.teduh.claims.documents.UploadedFile;

/**
 * /api/claims. Thin on purpose: turn JSON into a service call and the result back into JSON.
 * Rules live in Claim and ClaimService; errors become HTTP answers in ClaimErrorHandler.
 *
 * Three layers of "may this person do this?":
 * 1. Spring Security's filters: every /api request needs a sign-in (401 otherwise).
 * 2. @PreAuthorize on each method: the right role (403 otherwise). A customer can't approve.
 * 3. The service and the claim: the right person. A customer only finds their own claims (404 otherwise),
 *    and only the assigned agent can act after a review starts (422 otherwise).
 */
@RestController
@RequestMapping("/api/claims")
class ClaimController {

	private final ClaimService service;

	ClaimController(ClaimService service) {
		this.service = service;
	}

	/**
	 * One multipart request: a "claim" part holding the JSON fields, and one "files" part per document.
	 * Multipart is how browsers send files; the JSON part keeps the fields typed and validated as before.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('CUSTOMER')")
	// springdoc can't see the 201 inside ResponseEntity.created(...), so the API document is told here.
	@ApiResponse(responseCode = "201", description = "The new claim, with its reference")
	public ResponseEntity<ClaimDetails> submit(@AuthenticationPrincipal SignedInUser me,
			@Valid @RequestPart("claim") SubmitClaimRequest body,
			@RequestPart(name = "files", required = false) List<MultipartFile> files) {
		Claim claim = service.submit(new NewClaim(me.id(), body.policyId(), body.claimType(), body.incidentDate(),
				body.amountClaimed(), body.description()), uploaded(files));
		return ResponseEntity.created(URI.create("/api/claims/" + claim.getId())).body(details(claim));
	}

	/**
	 * A customer gets their own claims (the home page). An agent gets one tab of the queue, Submitted
	 * unless ?tab says otherwise. Customers have no queue, so ?tab from a customer is refused.
	 */
	@GetMapping
	public List<ClaimSummary> list(@AuthenticationPrincipal SignedInUser me,
			@RequestParam(required = false) String tab) {
		List<Claim> claims;
		if (me.isAgent()) {
			QueueTab queueTab = (tab == null) ? QueueTab.SUBMITTED : QueueTab.fromUrlName(tab)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown queue tab: " + tab));
			claims = service.queue(queueTab, me.id());
		}
		else {
			if (tab != null) {
				throw new AccessDeniedException("Only agents have a queue.");
			}
			claims = service.claimsOf(me.id());
		}
		return claims.stream().map(ClaimSummary::of).toList();
	}

	/** Agents can open any claim. A customer opening someone else's claim gets the same 404 as a made-up id. */
	@GetMapping("/{id}")
	public ClaimDetails get(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id) {
		return details(me.isAgent() ? service.find(id) : service.findForCustomer(id, me.id()));
	}

	/**
	 * A link that downloads one document and stops working after 15 minutes. The same who-may-see-this check
	 * as the claim page runs first, so someone else's document answers 404 like someone else's claim.
	 */
	@GetMapping("/{id}/documents/{documentId}/link")
	public DownloadLink documentLink(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@PathVariable UUID documentId) {
		Claim claim = me.isAgent() ? service.find(id) : service.findForCustomer(id, me.id());
		return service.linkFor(claim.getId(), documentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
	}

	@PostMapping("/{id}/start-review")
	@PreAuthorize("hasRole('AGENT')")
	public ClaimDetails startReview(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@Valid @RequestBody StartReviewRequest body) {
		return details(service.startReview(id, body.version(), me.id()));
	}

	@PostMapping("/{id}/request-info")
	@PreAuthorize("hasRole('AGENT')")
	public ClaimDetails requestInfo(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@Valid @RequestBody RequestInfoRequest body) {
		return details(service.requestInfo(id, body.version(), me.id(), body.message()));
	}

	/** Multipart like submit: a "response" part with the version and note, and up to five "files" parts. */
	@PostMapping(path = "/{id}/respond", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('CUSTOMER')")
	public ClaimDetails respond(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@Valid @RequestPart("response") RespondRequest body,
			@RequestPart(name = "files", required = false) List<MultipartFile> files) {
		return details(service.respond(id, body.version(), me.id(), body.note(), uploaded(files)));
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasRole('AGENT')")
	public ClaimDetails approve(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@Valid @RequestBody ApproveRequest body) {
		return details(service.approve(id, body.version(), me.id(), body.amount(), body.reason()));
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasRole('AGENT')")
	public ClaimDetails reject(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id,
			@Valid @RequestBody RejectRequest body) {
		return details(service.reject(id, body.version(), me.id(), body.reason()));
	}

	private ClaimDetails details(Claim claim) {
		return ClaimDetails.of(claim, service.documents(claim.getId()), service.timeline(claim.getId()));
	}

	/** Reads each file into memory. Fine at 10 MB a file and 5 files a request, the limits in application.yaml. */
	private static List<UploadedFile> uploaded(List<MultipartFile> files) {
		if (files == null) {
			return List.of();
		}
		return files.stream().map(file -> {
			try {
				return new UploadedFile(file.getOriginalFilename(), file.getBytes());
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).toList();
	}

}
