package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import my.teduh.claims.documents.StoredDocument;
import my.teduh.claims.policies.ClaimType;
import my.teduh.claims.policies.PolicyCover;

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

	/**
	 * One row of a list: a customer's claims or a queue tab. The policy id lets a list show the plan's name;
	 * the approved amount lets an approved claim show what was paid rather than what was asked; the customer's
	 * name is what an agent's queue shows.
	 */
	public record ClaimSummary(UUID id, String reference, UUID policyId, String customerName, ClaimType claimType,
			BigDecimal amountClaimed, @Schema(nullable = true) BigDecimal approvedAmount, ClaimStatus status,
			@Schema(nullable = true) UUID assignedAgentId, Instant submittedAt, long version) {

		/** names: display names by person id, from NameLookup. */
		static ClaimSummary of(Claim claim, Map<UUID, String> names) {
			return new ClaimSummary(claim.getId(), claim.getReference(), claim.getPolicyId(),
					nameOf(claim.getCustomerId(), names), claim.getClaimType(), claim.getAmountClaimed(),
					claim.getApprovedAmount(), claim.getStatus(), claim.getAssignedAgentId(), claim.getSubmittedAt(),
					claim.getVersion());
		}

	}

	/**
	 * One claim page: everything about the claim, its policy, its documents, and its timeline, oldest first.
	 * The policy is here because agents can't read /api/policies, and the review needs the cover and the limit.
	 */
	public record ClaimDetails(UUID id, String reference, UUID policyId, PolicyFacts policy, String customerName,
			ClaimType claimType,
			LocalDate incidentDate, BigDecimal amountClaimed, String description, ClaimStatus status,
			@Schema(nullable = true) UUID assignedAgentId, @Schema(nullable = true) String assignedAgentName,
			@Schema(nullable = true) BigDecimal approvedAmount, @Schema(nullable = true) String decisionReason,
			Instant submittedAt, long version, List<StoredDocument> documents, List<TimelineEntry> timeline) {

		/** names: display names by person id, from NameLookup, covering the customer, agent and every actor. */
		static ClaimDetails of(Claim claim, PolicyCover policy, List<StoredDocument> documents,
				List<ClaimEvent> events, Map<UUID, String> names) {
			UUID agent = claim.getAssignedAgentId();
			return new ClaimDetails(claim.getId(), claim.getReference(), claim.getPolicyId(),
					PolicyFacts.of(policy, claim.getClaimType()), nameOf(claim.getCustomerId(), names), claim.getClaimType(), claim.getIncidentDate(),
					claim.getAmountClaimed(), claim.getDescription(), claim.getStatus(), agent,
					(agent == null) ? null : nameOf(agent, names), claim.getApprovedAmount(), claim.getDecisionReason(),
					claim.getSubmittedAt(), claim.getVersion(), documents,
					events.stream().map(event -> TimelineEntry.of(event, names)).toList());
		}

	}

	/**
	 * What a claim page shows about the policy: its number and plan, the cover dates, and the limit for this
	 * claim's type (null if the policy doesn't cover that type, which the submit rules never let happen).
	 */
	public record PolicyFacts(String policyNumber, String planName, LocalDate coverStart, LocalDate coverEnd,
			@Schema(nullable = true) BigDecimal limit) {

		static PolicyFacts of(PolicyCover policy, ClaimType type) {
			return new PolicyFacts(policy.policyNumber(), policy.planName(), policy.coverStart(), policy.coverEnd(),
					policy.limitFor(type).orElse(null));
		}

	}

	/** fromStatus is null on the first line (the claim was submitted); message is only there when one was given. */
	public record TimelineEntry(@Schema(nullable = true) ClaimStatus fromStatus, ClaimStatus toStatus, UUID actorId,
			String actorName, @Schema(nullable = true) String message, Instant occurredAt) {

		static TimelineEntry of(ClaimEvent event, Map<UUID, String> names) {
			return new TimelineEntry(event.getFromStatus(), event.getToStatus(), event.getActorId(),
					nameOf(event.getActorId(), names), event.getMessage(), event.getOccurredAt());
		}

	}

	/** Every person on a claim has an account (the foreign keys see to that), so the fallback is only a guard. */
	private static String nameOf(UUID id, Map<UUID, String> names) {
		return names.getOrDefault(id, "Unknown");
	}

}
