package my.teduh.claims.claims;

/**
 * Thrown when someone tries a move the claim rules don't allow,
 * such as starting a review twice. The API will turn it into a 422 response.
 */
public class ClaimRuleException extends RuntimeException {

	public ClaimRuleException(String message) {
		super(message);
	}

}
