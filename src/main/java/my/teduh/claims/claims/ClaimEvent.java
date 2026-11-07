package my.teduh.claims.claims;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One line of a claim's timeline: what changed, who did it, when, and the message or reason.
 * Events are only ever added, never edited.
 */
@Entity
@Table(name = "claim_events")
public class ClaimEvent {

	/** Numbered by the database, so events that happen in the same millisecond still keep their order. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private UUID claimId;

	/** Null for the first event, when the claim is submitted. */
	@Enumerated(EnumType.STRING)
	private ClaimStatus fromStatus;

	@Enumerated(EnumType.STRING)
	private ClaimStatus toStatus;

	private UUID actorId;

	private String message;

	private Instant occurredAt;

	protected ClaimEvent() {
	}

	private ClaimEvent(UUID claimId, ClaimStatus fromStatus, ClaimStatus toStatus, UUID actorId, String message,
			Instant occurredAt) {
		this.claimId = claimId;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.actorId = actorId;
		this.message = message;
		this.occurredAt = occurredAt;
	}

	/** The first line of every timeline: the customer submitted the claim. */
	public static ClaimEvent submitted(Claim claim) {
		return new ClaimEvent(claim.getId(), null, ClaimStatus.SUBMITTED, claim.getCustomerId(), null,
				claim.getSubmittedAt());
	}

	/** A status change returned by one of Claim's actions, stamped with the time it happened. */
	public static ClaimEvent of(UUID claimId, ClaimTransition transition, Instant occurredAt) {
		return new ClaimEvent(claimId, transition.from(), transition.to(), transition.actorId(), transition.message(),
				occurredAt);
	}

	public Long getId() {
		return id;
	}

	public UUID getClaimId() {
		return claimId;
	}

	public ClaimStatus getFromStatus() {
		return fromStatus;
	}

	public ClaimStatus getToStatus() {
		return toStatus;
	}

	public UUID getActorId() {
		return actorId;
	}

	public String getMessage() {
		return message;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

}
