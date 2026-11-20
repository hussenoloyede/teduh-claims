package my.teduh.claims.policies;

/** A policy that can't be bought as asked, such as cover starting yesterday. The API answers 422. */
class PolicyRuleException extends RuntimeException {

	PolicyRuleException(String message) {
		super(message);
	}

}
