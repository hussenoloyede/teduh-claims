package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A travel insurance claim and the rules for moving it from one status to the next.
 * Plain Java on purpose: no Spring and no database here, so every rule can be unit tested in milliseconds.
 *
 * <pre>
 * SUBMITTED --start review--> UNDER_REVIEW --approve--> APPROVED
 *                               |      ^     \--reject--> REJECTED
 *                  request info |      | customer responds
 *                               v      |
 *                           MORE_INFO_NEEDED
 * </pre>
 *
 * Every action checks, in this order: the claim is in the right status, the right person is acting,
 * and what they sent is valid. A refused action changes nothing.
 *
 * The JPA annotations only tell Hibernate how to store the fields. They don't change the rules, and the
 * class still works without a database, which is how ClaimTest uses it. Column names come from the field
 * names: Spring Boot turns amountClaimed into amount_claimed.
 */
@Entity
@Table(name = "claims")
public class Claim {

	static final int MAX_MESSAGE_LENGTH = 1_000;
	static final int MAX_NOTE_LENGTH = 500;
	static final int MAX_DESCRIPTION_LENGTH = 500;

	// Not final: Hibernate sets fields when it loads a claim from the database.
	@Id
	private UUID id;

	private String reference;

	private UUID customerId;

	private UUID policyId;

	@Enumerated(EnumType.STRING)
	private ClaimType claimType;

	private LocalDate incidentDate;

	private BigDecimal amountClaimed;

	private String description;

	@Enumerated(EnumType.STRING)
	private ClaimStatus status;

	private UUID assignedAgentId;

	private BigDecimal approvedAmount;

	private String decisionReason;

	private Instant submittedAt;

	/**
	 * Goes up by one on every save. Hibernate adds "and version = ?" to each update, so a save based on
	 * an out-of-date copy matches no row and fails instead of overwriting someone else's change.
	 * Null until the first save, which is also how Spring Data knows the claim is new.
	 */
	@Version
	private Long version;

	/** For Hibernate only. Code creates claims through submit(). */
	protected Claim() {
	}

	private Claim(UUID id, String reference, NewClaim details, String description, Instant submittedAt) {
		this.id = id;
		this.reference = reference;
		this.customerId = details.customerId();
		this.policyId = details.policyId();
		this.claimType = details.claimType();
		this.incidentDate = details.incidentDate();
		this.amountClaimed = details.amountClaimed();
		this.description = description;
		this.submittedAt = submittedAt;
		this.status = ClaimStatus.SUBMITTED;
	}

	/** A customer submits a new claim. It always starts as SUBMITTED, with no agent. */
	public static Claim submit(String reference, NewClaim details, Instant submittedAt) {
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(details, "details");
		Objects.requireNonNull(submittedAt, "submittedAt");
		Objects.requireNonNull(details.customerId(), "customerId");
		Objects.requireNonNull(details.policyId(), "policyId");
		Objects.requireNonNull(details.claimType(), "claimType");
		Objects.requireNonNull(details.incidentDate(), "incidentDate");
		Objects.requireNonNull(details.amountClaimed(), "amountClaimed");
		if (details.amountClaimed().signum() <= 0) {
			throw new ClaimRuleException("The amount claimed must be more than zero.");
		}
		requireTwoDecimals(details.amountClaimed());
		String description = requireText(details.description(), MAX_DESCRIPTION_LENGTH,
				"Tell us what happened.", "The description can be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
		return new Claim(UUID.randomUUID(), reference, details, description, submittedAt);
	}

	/** Any agent can pick up a submitted claim. From then on only that agent can act on it. */
	public ClaimTransition startReview(UUID agentId) {
		Objects.requireNonNull(agentId, "agentId");
		requireStatus(ClaimStatus.SUBMITTED, "start a review");
		assignedAgentId = agentId;
		return moveTo(ClaimStatus.UNDER_REVIEW, agentId, null);
	}

	/** The assigned agent asks the customer for something. There can be several rounds. */
	public ClaimTransition requestInfo(UUID agentId, String message) {
		requireStatus(ClaimStatus.UNDER_REVIEW, "ask for more information");
		requireAssignedAgent(agentId);
		String text = requireText(message, MAX_MESSAGE_LENGTH, "A message to the customer is required.",
				"The message can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		return moveTo(ClaimStatus.MORE_INFO_NEEDED, agentId, text);
	}

	/**
	 * The customer answers with a note, at least one file, or both.
	 * The files themselves are the documents module's job; the claim only needs to know some arrived.
	 * The claim goes back to the same agent.
	 */
	public ClaimTransition respond(UUID respondingCustomerId, String note, boolean hasFiles) {
		requireStatus(ClaimStatus.MORE_INFO_NEEDED, "respond");
		if (!customerId.equals(respondingCustomerId)) {
			throw new ClaimRuleException("Only the customer who made this claim can respond.");
		}
		boolean hasNote = note != null && !note.isBlank();
		if (!hasNote && !hasFiles) {
			throw new ClaimRuleException("Add a note, a file, or both.");
		}
		String text = hasNote ? requireText(note, MAX_NOTE_LENGTH, null,
				"The note can be at most " + MAX_NOTE_LENGTH + " characters.") : null;
		return moveTo(ClaimStatus.UNDER_REVIEW, respondingCustomerId, text);
	}

	/**
	 * The assigned agent approves an amount above zero and no higher than the amount claimed.
	 * Approving less than claimed needs a reason, which the customer sees.
	 */
	public ClaimTransition approve(UUID agentId, BigDecimal amount, String reason) {
		requireStatus(ClaimStatus.UNDER_REVIEW, "approve");
		requireAssignedAgent(agentId);
		Objects.requireNonNull(amount, "amount");
		if (amount.signum() <= 0) {
			throw new ClaimRuleException("The approved amount must be more than zero.");
		}
		requireTwoDecimals(amount);
		if (amount.compareTo(amountClaimed) > 0) {
			throw new ClaimRuleException("You can't approve more than the RM " + amountClaimed.toPlainString() + " claimed.");
		}
		boolean lessThanClaimed = amount.compareTo(amountClaimed) < 0;
		String text = null;
		if (lessThanClaimed) {
			text = requireText(reason, MAX_MESSAGE_LENGTH, "A reason is required when approving less than the amount claimed.",
					"The reason can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		}
		else if (reason != null && !reason.isBlank()) {
			text = requireText(reason, MAX_MESSAGE_LENGTH, null,
					"The reason can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		}
		approvedAmount = amount;
		decisionReason = text;
		return moveTo(ClaimStatus.APPROVED, agentId, text);
	}

	/** The assigned agent rejects the claim. A reason is always required, and the customer sees it. */
	public ClaimTransition reject(UUID agentId, String reason) {
		requireStatus(ClaimStatus.UNDER_REVIEW, "reject");
		requireAssignedAgent(agentId);
		String text = requireText(reason, MAX_MESSAGE_LENGTH, "A reason is required to reject a claim.",
				"The reason can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		decisionReason = text;
		return moveTo(ClaimStatus.REJECTED, agentId, text);
	}

	private void requireStatus(ClaimStatus expected, String action) {
		if (status != expected) {
			throw new ClaimRuleException("Can't " + action + " on a claim that is " + status.label() + ".");
		}
	}

	private void requireAssignedAgent(UUID agentId) {
		if (assignedAgentId == null || !assignedAgentId.equals(agentId)) {
			throw new ClaimRuleException("Only the agent assigned to this claim can do that.");
		}
	}

	private static void requireTwoDecimals(BigDecimal amount) {
		if (amount.scale() > 2) {
			throw new ClaimRuleException("Amounts can have at most two decimal places.");
		}
	}

	/** Trims the text and checks it. A null whenMissing means the caller already knows it isn't blank. */
	private static String requireText(String text, int maxLength, String whenMissing, String whenTooLong) {
		if (text == null || text.isBlank()) {
			throw new ClaimRuleException(whenMissing);
		}
		String trimmed = text.strip();
		if (trimmed.length() > maxLength) {
			throw new ClaimRuleException(whenTooLong);
		}
		return trimmed;
	}

	private ClaimTransition moveTo(ClaimStatus next, UUID actorId, String message) {
		ClaimTransition transition = new ClaimTransition(status, next, actorId, message);
		status = next;
		return transition;
	}

	public UUID getId() {
		return id;
	}

	public String getReference() {
		return reference;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public UUID getPolicyId() {
		return policyId;
	}

	public ClaimType getClaimType() {
		return claimType;
	}

	public LocalDate getIncidentDate() {
		return incidentDate;
	}

	public String getDescription() {
		return description;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	/** The version this copy was loaded at. Every change request will send it back. */
	public Long getVersion() {
		return version;
	}

	public BigDecimal getAmountClaimed() {
		return amountClaimed;
	}

	public ClaimStatus getStatus() {
		return status;
	}

	public UUID getAssignedAgentId() {
		return assignedAgentId;
	}

	/** Null until the claim is approved. */
	public BigDecimal getApprovedAmount() {
		return approvedAmount;
	}

	/** The reason the customer sees: required for a rejection or a partial approval, otherwise null. */
	public String getDecisionReason() {
		return decisionReason;
	}

}
