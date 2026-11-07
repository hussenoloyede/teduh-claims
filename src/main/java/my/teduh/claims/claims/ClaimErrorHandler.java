package my.teduh.claims.claims;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns the claims module's exceptions into RFC 9457 problem-details answers, the same shape ToolFleet used.
 * The frontend shows "detail" to the person; it is written for them, not for developers.
 */
@RestControllerAdvice
class ClaimErrorHandler {

	/** 422: the request was well formed but broke a claim rule, for example approving more than was claimed. */
	@ExceptionHandler(ClaimRuleException.class)
	ProblemDetail ruleBroken(ClaimRuleException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
		problem.setTitle("Not allowed");
		return problem;
	}

	/** 409: someone changed the claim after this person loaded it. The page reloads and shows the latest. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail changedMeanwhile(OptimisticLockingFailureException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"This claim changed while you were looking at it. Reload to see the latest.");
		problem.setTitle("Claim changed");
		return problem;
	}

	@ExceptionHandler(ClaimNotFoundException.class)
	ProblemDetail notFound(ClaimNotFoundException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Claim not found.");
		problem.setTitle("Claim not found");
		return problem;
	}

}
