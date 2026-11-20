package my.teduh.claims.policies;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The policies module's refusals, in the same problem-details shape as the claims module's. */
@RestControllerAdvice
class PolicyErrorHandler {

	@ExceptionHandler(PolicyRuleException.class)
	ProblemDetail ruleBroken(PolicyRuleException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
		problem.setTitle("Not allowed");
		return problem;
	}

}
