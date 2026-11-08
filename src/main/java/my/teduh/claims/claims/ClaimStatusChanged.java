package my.teduh.claims.claims;

import java.util.UUID;

/**
 * Published every time a claim changes status, including when it is first submitted (from is null).
 * Other modules listen to this instead of calling into the claims module; the notifications module turns it
 * into "your claim was approved" and similar.
 *
 * Spring Modulith stores each event in the event_publication table in the same transaction as the change,
 * as JSON, so it is a plain record of plain values: ids, the reference, the statuses and the message.
 */
public record ClaimStatusChanged(UUID claimId, String reference, UUID customerId, UUID assignedAgentId,
		ClaimStatus from, ClaimStatus to, UUID actorId, String message) {

	static ClaimStatusChanged of(Claim claim, ClaimTransition transition) {
		return new ClaimStatusChanged(claim.getId(), claim.getReference(), claim.getCustomerId(),
				claim.getAssignedAgentId(), transition.from(), transition.to(), transition.actorId(),
				transition.message());
	}

}
