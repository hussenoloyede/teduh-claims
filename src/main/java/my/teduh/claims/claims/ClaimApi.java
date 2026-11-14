package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import my.teduh.claims.documents.StoredDocument;
import my.teduh.claims.policies.ClaimType;

/**
 * The shapes of the JSON that goes in and out of /api/claims. Records keep them short and immutable.
 * Entities never leave the module as JSON: what the API shows is chosen here, field by field.
 *
 * @NotNull only checks the request is complete (otherwise 400). Whether the values break a rule is the
 * claim's job (422), so each rule lives in one place.
 *
 * @Schema(nullable = true) marks a field that may be null or left out. Every other field is always there
 * (ApiDocs makes that the rule in the OpenAPI document), so the generated TypeScript types only make the
 * React app check for a missing value where one can really be missing.
 */
public final class ClaimApi {

	private ClaimApi() {
	}

	public record SubmitClaimRequest(@NotNull UUID policyId, @NotNull ClaimType claimType,
			@NotNull LocalDate incidentDate, @NotNull BigDecimal amountClaimed,
			@Schema(nullable = true) String description) {
	}

	/** Every change sends back the version the person was looking at. */
	public record StartReviewRequest(@NotNull Long version) {
	}

	public record RequestInfoRequest(@NotNull Long version, @Schema(nullable = true) String message) {
	}

	/** Sent as the "response" part of a multipart request; the files travel as "files" parts beside it. */
	public record RespondRequest(@NotNull Long version, @Schema(nullable = true) String note) {
	}

	public record ApproveRequest(@NotNull Long version, @NotNull BigDecimal amount,
			@Schema(nullable = true) String reason) {
	}

	public record RejectRequest(@NotNull Long version, @Schema(nullable = true) String reason) {
	}

	/** One row of a list: a customer's claims or a queue tab. */
	public record ClaimSummary(UUID id, String reference, ClaimType claimType, BigDecimal amountClaimed,
			ClaimStatus status, @Schema(nullable = true) UUID assignedAgentId, Instant submittedAt, long version) {

		static ClaimSummary of(Claim claim) {
			return new ClaimSummary(claim.getId(), claim.getReference(), claim.getClaimType(),
					claim.getAmountClaimed(), claim.getStatus(), claim.getAssignedAgentId(), claim.getSubmittedAt(),
					claim.getVersion());
		}

	}

	/** One claim page: everything about the claim, its documents, and its timeline, oldest first. */
	public record ClaimDetails(UUID id, String reference, UUID policyId, ClaimType claimType, LocalDate incidentDate,
			BigDecimal amountClaimed, String description, ClaimStatus status,
			@Schema(nullable = true) UUID assignedAgentId, @Schema(nullable = true) BigDecimal approvedAmount,
			@Schema(nullable = true) String decisionReason, Instant submittedAt, long version,
			List<StoredDocument> documents, List<TimelineEntry> timeline) {

		static ClaimDetails of(Claim claim, List<StoredDocument> documents, List<ClaimEvent> events) {
			return new ClaimDetails(claim.getId(), claim.getReference(), claim.getPolicyId(), claim.getClaimType(),
					claim.getIncidentDate(), claim.getAmountClaimed(), claim.getDescription(), claim.getStatus(),
					claim.getAssignedAgentId(), claim.getApprovedAmount(), claim.getDecisionReason(),
					claim.getSubmittedAt(), claim.getVersion(), documents,
					events.stream().map(TimelineEntry::of).toList());
		}

	}

	/** fromStatus is null on the first line (the claim was submitted); message is only there when one was given. */
	public record TimelineEntry(@Schema(nullable = true) ClaimStatus fromStatus, ClaimStatus toStatus, UUID actorId,
			@Schema(nullable = true) String message, Instant occurredAt) {

		static TimelineEntry of(ClaimEvent event) {
			return new TimelineEntry(event.getFromStatus(), event.getToStatus(), event.getActorId(),
					event.getMessage(), event.getOccurredAt());
		}

	}

}
