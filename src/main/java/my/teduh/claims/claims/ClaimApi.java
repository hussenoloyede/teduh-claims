package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import my.teduh.claims.documents.StoredDocument;
import my.teduh.claims.policies.ClaimType;

/**
 * The shapes of the JSON that goes in and out of /api/claims. Records keep them short and immutable.
 * Entities never leave the module as JSON: what the API shows is chosen here, field by field.
 *
 * @NotNull only checks the request is complete (otherwise 400). Whether the values break a rule is the
 * claim's job (422), so each rule lives in one place.
 */
public final class ClaimApi {

	private ClaimApi() {
	}

	public record SubmitClaimRequest(@NotNull UUID policyId, @NotNull ClaimType claimType,
			@NotNull LocalDate incidentDate, @NotNull BigDecimal amountClaimed, String description) {
	}

	/** Every change sends back the version the person was looking at. */
	public record StartReviewRequest(@NotNull Long version) {
	}

	public record RequestInfoRequest(@NotNull Long version, String message) {
	}

	/** Sent as the "response" part of a multipart request; the files travel as "files" parts beside it. */
	public record RespondRequest(@NotNull Long version, String note) {
	}

	public record ApproveRequest(@NotNull Long version, @NotNull BigDecimal amount, String reason) {
	}

	public record RejectRequest(@NotNull Long version, String reason) {
	}

	/** One row of a list: a customer's claims or a queue tab. */
	public record ClaimSummary(UUID id, String reference, ClaimType claimType, BigDecimal amountClaimed,
			ClaimStatus status, UUID assignedAgentId, Instant submittedAt, long version) {

		static ClaimSummary of(Claim claim) {
			return new ClaimSummary(claim.getId(), claim.getReference(), claim.getClaimType(),
					claim.getAmountClaimed(), claim.getStatus(), claim.getAssignedAgentId(), claim.getSubmittedAt(),
					claim.getVersion());
		}

	}

	/** One claim page: everything about the claim, its documents, and its timeline, oldest first. */
	public record ClaimDetails(UUID id, String reference, UUID policyId, ClaimType claimType, LocalDate incidentDate,
			BigDecimal amountClaimed, String description, ClaimStatus status, UUID assignedAgentId,
			BigDecimal approvedAmount, String decisionReason, Instant submittedAt, long version,
			List<StoredDocument> documents, List<TimelineEntry> timeline) {

		static ClaimDetails of(Claim claim, List<StoredDocument> documents, List<ClaimEvent> events) {
			return new ClaimDetails(claim.getId(), claim.getReference(), claim.getPolicyId(), claim.getClaimType(),
					claim.getIncidentDate(), claim.getAmountClaimed(), claim.getDescription(), claim.getStatus(),
					claim.getAssignedAgentId(), claim.getApprovedAmount(), claim.getDecisionReason(),
					claim.getSubmittedAt(), claim.getVersion(), documents,
					events.stream().map(TimelineEntry::of).toList());
		}

	}

	public record TimelineEntry(ClaimStatus fromStatus, ClaimStatus toStatus, UUID actorId, String message,
			Instant occurredAt) {

		static TimelineEntry of(ClaimEvent event) {
			return new TimelineEntry(event.getFromStatus(), event.getToStatus(), event.getActorId(),
					event.getMessage(), event.getOccurredAt());
		}

	}

}
