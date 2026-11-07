package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
 */
public class Claim {

	static final int MAX_MESSAGE_LENGTH = 1_000;
	static final int MAX_NOTE_LENGTH = 500;

	private final UUID id;
	private final UUID customerId;
	private final BigDecimal amountClaimed;
	private ClaimStatus status;
	private UUID assignedAgentId;
	private BigDecimal approvedAmount;
	private String decisionReason;

	private Claim(UUID id, UUID customerId, BigDecimal amountClaimed) {
		this.id = id;
		this.customerId = customerId;
		this.amountClaimed = amountClaimed;
		this.status = ClaimStatus.SUBMITTED;
	}

	/** A customer submits a new claim. It always starts as SUBMITTED, with no agent. */
	public static Claim submit(UUID customerId, BigDecimal amountClaimed) {
		Objects.requireNonNull(customerId, "customerId");
		Objects.requireNonNull(amountClaimed, "amountClaimed");
		if (amountClaimed.signum() <= 0) {
			throw new ClaimRuleException("The amount claimed must be more than zero.");
		}
		requireTwoDecimals(amountClaimed);
		return new Claim(UUID.randomUUID(), customerId, amountClaimed);
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

	public UUID getCustomerId() {
		return customerId;
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
