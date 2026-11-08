package my.teduh.claims.claims;

import static my.teduh.claims.claims.ClaimController.DEMO_USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.TestData;
import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.shared.MalaysianTime;

/**
 * The /api/claims endpoints over HTTP, against real PostgreSQL. MockMvc sends requests through the whole
 * Spring web stack (JSON, validation, error handling) without opening a network port.
 * Every test makes its own users and policy, so the shared queue never confuses one test with another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClaimApiTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private TestData data;

	private UUID aisyah;
	private UUID farid;
	private UUID meiLing;

	private final LocalDate today = LocalDate.now(MalaysianTime.ZONE);

	@BeforeEach
	void peopleExist() {
		data = new TestData(jdbc);
		aisyah = data.customer();
		farid = data.agent();
		meiLing = data.agent();
	}

	@Test
	void theDemoStoryOverHttp() {
		String id = submitAisyahsClaim();

		MvcTestResult result = post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");
		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(read(result, "$.status")).isEqualTo("UNDER_REVIEW");
		assertThat(read(result, "$.assignedAgentId")).isEqualTo(farid.toString());

		result = post("/api/claims/" + id + "/request-info", farid, """
				{"version": 1, "message": "Please upload the airline's delay report."}""");
		assertThat(read(result, "$.status")).isEqualTo("MORE_INFO_NEEDED");

		result = post("/api/claims/" + id + "/respond", aisyah, """
				{"version": 2, "note": "Here is the delay report."}""");
		assertThat(read(result, "$.status")).isEqualTo("UNDER_REVIEW");

		result = post("/api/claims/" + id + "/approve", farid, """
				{"version": 3, "amount": 400.00, "reason": "Receipts total RM 400."}""");
		assertThat(result).hasStatus(HttpStatus.OK);

		MvcTestResult page = mvc.get().uri("/api/claims/" + id).exchange();
		assertThat(page).hasStatus(HttpStatus.OK);
		assertThat(read(page, "$.status")).isEqualTo("APPROVED");
		assertThat(new BigDecimal(read(page, "$.approvedAmount").toString())).isEqualByComparingTo("400");
		assertThat(read(page, "$.decisionReason")).isEqualTo("Receipts total RM 400.");
		assertThat(read(page, "$.version")).isEqualTo(4);
		assertThat(strings(page, "$.timeline[*].toStatus"))
			.containsExactly("SUBMITTED", "UNDER_REVIEW", "MORE_INFO_NEEDED", "UNDER_REVIEW", "APPROVED");
	}

	@Test
	void submittingAnswers201WithTheClaimsAddress() {
		MvcTestResult result = post("/api/claims", aisyah, submitBody(travelPlusFor(aisyah), "450.00"));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		String id = (String) read(result, "$.id");
		assertThat(result).headers().hasValue("Location", "/api/claims/" + id);
		assertThat((String) read(result, "$.reference")).matches("CLM-\\d{4}-\\d{6}");
		assertThat(read(result, "$.status")).isEqualTo("SUBMITTED");
	}

	@Test
	void aBrokenRuleAnswers422WithAMessageForThePerson() {
		String id = submitAisyahsClaim();
		post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");

		MvcTestResult result = post("/api/claims/" + id + "/approve", farid, """
				{"version": 1, "amount": 450.01}""");

		assertThat(result).hasStatus(422);
		assertThat(read(result, "$.title")).isEqualTo("Not allowed");
		assertThat(read(result, "$.detail")).isEqualTo("You can't approve more than the RM 450.00 claimed.");
	}

	@Test
	void twoAgentsActingOnTheSameVersionTheSecondGets409() {
		String id = submitAisyahsClaim();

		MvcTestResult first = post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");
		MvcTestResult second = post("/api/claims/" + id + "/start-review", meiLing, "{\"version\": 0}");

		assertThat(first).hasStatus(HttpStatus.OK);
		assertThat(second).hasStatus(HttpStatus.CONFLICT);
		assertThat(read(second, "$.detail"))
			.isEqualTo("This claim changed while you were looking at it. Reload to see the latest.");
	}

	@Test
	void anUnknownClaimAnswers404() {
		MvcTestResult result = mvc.get().uri("/api/claims/" + UUID.randomUUID()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(read(result, "$.detail")).isEqualTo("Claim not found.");
	}

	@Test
	void anIncompleteRequestAnswers400() {
		MvcTestResult result = post("/api/claims", aisyah, """
				{"claimType": "BAGGAGE_DELAYED", "incidentDate": "%s", "amountClaimed": 450.00}"""
			.formatted(today.minusDays(10)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void customersSeeOnlyTheirOwnClaims() {
		String id = submitAisyahsClaim();

		assertThat(idsIn(mvc.get().uri("/api/claims").header(DEMO_USER, aisyah.toString()).exchange()))
			.contains(id);
		assertThat(idsIn(mvc.get().uri("/api/claims").header(DEMO_USER, UUID.randomUUID().toString()).exchange()))
			.doesNotContain(id);
	}

	@Test
	void aClaimMovesBetweenQueueTabsAsItIsWorked() {
		String id = submitAisyahsClaim();
		assertThat(idsIn(queue("submitted", farid))).contains(id);

		post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");

		assertThat(idsIn(queue("submitted", farid))).doesNotContain(id);
		assertThat(idsIn(queue("in-review-by-me", farid))).contains(id);
		assertThat(idsIn(queue("in-review-by-me", meiLing))).doesNotContain(id);

		post("/api/claims/" + id + "/request-info", farid, """
				{"version": 1, "message": "Please upload the airline's delay report."}""");

		assertThat(idsIn(queue("in-review-by-me", farid))).doesNotContain(id);
		assertThat(idsIn(queue("waiting-on-customer", farid))).contains(id);
	}

	@Test
	void anUnknownQueueTabAnswers400() {
		assertThat(queue("everything", farid)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	// Helpers

	private String submitAisyahsClaim() {
		MvcTestResult result = post("/api/claims", aisyah, submitBody(travelPlusFor(aisyah), "450.00"));
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return (String) read(result, "$.id");
	}

	private String submitBody(UUID policy, String amount) {
		return """
				{"policyId": "%s", "claimType": "BAGGAGE_DELAYED", "incidentDate": "%s",
				 "amountClaimed": %s, "description": "My checked luggage arrived 48 hours late."}"""
			.formatted(policy, today.minusDays(10), amount);
	}

	/** A policy covering 60 days either side of today, so the test works whatever day it runs. */
	private UUID travelPlusFor(UUID customer) {
		return data.travelPlus(customer, today.minusDays(60), today.plusDays(60));
	}

	private MvcTestResult post(String path, UUID user, String json) {
		return mvc.post()
			.uri(path)
			.header(DEMO_USER, user.toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)
			.exchange();
	}

	private MvcTestResult queue(String tab, UUID agent) {
		return mvc.get().uri("/api/claims?tab=" + tab).header(DEMO_USER, agent.toString()).exchange();
	}

	private static Object read(MvcTestResult result, String path) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), path);
		}
		catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> strings(MvcTestResult result, String path) {
		return (List<String>) read(result, path);
	}

	private static List<String> idsIn(MvcTestResult result) {
		assertThat(result).hasStatus(HttpStatus.OK);
		return strings(result, "$[*].id");
	}

}
