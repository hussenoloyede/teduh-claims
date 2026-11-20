package my.teduh.claims;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.accounts.DemoAccounts;
import my.teduh.claims.shared.MalaysianTime;

/**
 * "Try as a new customer" end to end over HTTP: a fresh account with no policy, the plans, buying one,
 * and claiming on it the same day. Sessions travel as the SESSION cookie, the way a browser sends them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NewCustomerTest {

	private static final byte[] PDF = "%PDF-1.7\n% made-up receipt for tests\n%%EOF\n".getBytes(US_ASCII);

	@Autowired
	private MockMvcTester mvc;

	private final LocalDate today = LocalDate.now(MalaysianTime.ZONE);

	@Test
	void aNewCustomerChoosesAPlanAndCanClaimTheSameDay() {
		Cookie session = newCustomer();

		MvcTestResult me = mvc.get().uri("/api/auth/me").cookie(session).exchange();
		assertThat(read(me, "$.displayName")).isEqualTo("Demo traveller");
		assertThat(read(me, "$.role")).isEqualTo("CUSTOMER");
		assertThat((String) read(me, "$.email")).startsWith("traveller-").endsWith("@example.com");
		assertThat(body(mvc.get().uri("/api/policies").cookie(session).exchange())).isEqualTo("[]");

		MvcTestResult plans = mvc.get().uri("/api/plans").cookie(session).exchange();
		assertThat(plans).hasStatus(HttpStatus.OK);
		assertThat((List<?>) read(plans, "$[*].plan")).containsExactly("ESSENTIAL", "PLUS", "MAX");
		assertThat(read(plans, "$[1].name")).isEqualTo("Teduh Travel Plus");
		assertThat(((Number) read(plans, "$[1].pricePerTrip")).doubleValue()).isEqualTo(49.0);
		assertThat(((Number) read(plans, "$[1].limits.BAGGAGE_DELAYED")).doubleValue()).isEqualTo(1000.0);

		MvcTestResult bought = buy(session, "PLUS", today, today.plusDays(13));
		assertThat(bought).hasStatus(HttpStatus.CREATED);
		assertThat((String) read(bought, "$.policyNumber")).matches("TTP-\\d{2}-\\d{6}");
		assertThat(read(bought, "$.planName")).isEqualTo("Teduh Travel Plus");
		assertThat(read(bought, "$.coverEnd")).isEqualTo(today.plusDays(13).toString());

		// The same trip twice would be two policies for the same days.
		MvcTestResult again = buy(session, "MAX", today.plusDays(5), today.plusDays(20));
		assertThat(again).hasStatus(422);
		assertThat(read(again, "$.detail")).isEqualTo("You already have cover for some of those dates.");

		// Cover starts today, so a claim for today is inside it.
		String policyId = (String) read(bought, "$.policyId");
		MvcTestResult claim = mvc.post()
			.uri("/api/claims")
			.multipart()
			.file(new MockMultipartFile("claim", "", MediaType.APPLICATION_JSON_VALUE, """
					{"policyId": "%s", "claimType": "BAGGAGE_DELAYED", "incidentDate": "%s",
					 "amountClaimed": 450.00, "description": "My bag arrived two days late."}"""
				.formatted(policyId, today).getBytes(UTF_8)))
			.file(new MockMultipartFile("files", "receipt.pdf", "application/pdf", PDF))
			.cookie(session)
			.with(BrowserCsrf.token(mvc))
			.exchange();
		assertThat(claim).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void coverDatesFollowTheRules() {
		Cookie session = newCustomer();

		assertThat(read(buy(session, "PLUS", today.minusDays(1), today.plusDays(5)), "$.detail"))
			.isEqualTo("Cover can't start before today.");
		assertThat(read(buy(session, "PLUS", today.plusDays(5), today.plusDays(4)), "$.detail"))
			.isEqualTo("The trip can't end before it starts.");
		assertThat(read(buy(session, "PLUS", today, today.plusDays(90)), "$.detail"))
			.isEqualTo("One policy covers at most 90 days.");
		// 90 days exactly, counting both the first and the last day, is allowed.
		assertThat(buy(session, "ESSENTIAL", today, today.plusDays(89))).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void agentsCantBuyOrSeePlans() {
		MvcTestResult signIn = mvc.post()
			.uri("/api/auth/login")
			.with(BrowserCsrf.token(mvc))
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", "farid@example.com")
			.param("password", DemoAccounts.PASSWORD)
			.exchange();
		Cookie farid = sessionCookie(signIn);

		assertThat(mvc.get().uri("/api/plans").cookie(farid).exchange()).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(buy(farid, "PLUS", today, today.plusDays(3))).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void creatingANewCustomerNeedsTheCsrfToken() {
		assertThat(mvc.post().uri("/api/auth/new-customer").exchange()).hasStatus(HttpStatus.FORBIDDEN);
	}

	// Helpers

	private Cookie newCustomer() {
		MvcTestResult result = mvc.post().uri("/api/auth/new-customer").with(BrowserCsrf.token(mvc)).exchange();
		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		return sessionCookie(result);
	}

	private MvcTestResult buy(Cookie session, String plan, LocalDate start, LocalDate end) {
		return mvc.post()
			.uri("/api/policies")
			.cookie(session)
			.with(BrowserCsrf.token(mvc))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"plan": "%s", "coverStart": "%s", "coverEnd": "%s"}""".formatted(plan, start, end))
			.exchange();
	}

	/** The last SESSION cookie the response sets, as a browser would keep it. */
	private static Cookie sessionCookie(MvcTestResult result) {
		String header = result.getResponse()
			.getHeaders("Set-Cookie")
			.stream()
			.filter(h -> h.startsWith("SESSION="))
			.reduce((first, second) -> second)
			.orElseThrow(() -> new AssertionError("No SESSION cookie"));
		return new Cookie("SESSION", header.substring("SESSION=".length()).split(";", 2)[0]);
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object read(MvcTestResult result, String path) {
		return JsonPath.read(body(result), path);
	}

}
