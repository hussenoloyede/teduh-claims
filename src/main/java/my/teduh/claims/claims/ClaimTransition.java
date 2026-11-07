package my.teduh.claims.claims;

import java.util.UUID;

/**
 * One status change: from, to, who did it, and the message or reason if there was one.
 * The claims service will stamp it with a time and save it to the claim's timeline.
 */
public record ClaimTransition(ClaimStatus from, ClaimStatus to, UUID actorId, String message) {
}
