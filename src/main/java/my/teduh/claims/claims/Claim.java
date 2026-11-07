package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A travel insurance claim and the rules for moving it from one status to the next.
 * Plain Java on purpose: no Spring and no database here, so every rule can be unit tested in milliseconds.
 */
public class Claim {

	private final UUID id;
	private final UUID customerId;
	private final BigDecimal amountClaimed;
	private ClaimStatus status;
	private UUID assignedAgentId;

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
		if (amountClaimed.scale() > 2) {
			throw new ClaimRuleException("Amounts can have at most two decimal places.");
		}
		return new Claim(UUID.randomUUID(), customerId, amountClaimed);
	}

	/** Any agent can pick up a submitted claim. From then on only that agent can act on it. */
	public ClaimTransition startReview(UUID agentId) {
		Objects.requireNonNull(agentId, "agentId");
		requireStatus(ClaimStatus.SUBMITTED, "start a review");
		assignedAgentId = agentId;
		return moveTo(ClaimStatus.UNDER_REVIEW, agentId, null);
	}

	private void requireStatus(ClaimStatus expected, String action) {
		if (status != expected) {
			throw new ClaimRuleException("Can't " + action + " on a claim that is " + status.label() + ".");
		}
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

}
