package my.teduh.claims.claims;

import java.util.UUID;

/** No claim with this id, or (from week 3) not one this user may see. The API will answer 404 either way. */
public class ClaimNotFoundException extends RuntimeException {

	public ClaimNotFoundException(UUID claimId) {
		super("Claim not found: " + claimId);
	}

}
