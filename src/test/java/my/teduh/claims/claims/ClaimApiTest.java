package my.teduh.claims.claims;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.BrowserCsrf;
import my.teduh.claims.TestData;
import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.accounts.Role;
import my.teduh.claims.accounts.SignedInUser;
import my.teduh.claims.shared.MalaysianTime;

/**
 * The /api/claims endpoints over HTTP, against real PostgreSQL and Azurite, with Spring Security switched on.
 * MockMvc sends requests through the whole stack (security filters, multipart, JSON, validation, error
 * handling) without opening a network port.
 *
 * user(...) puts a signed-in person on the request without going through the sign-in form; SignInTest
 * covers the real sign-in. Changes carry a real CSRF token, fetched the way the browser does (BrowserCsrf).
 * Every test makes its own people and policy, so the shared queue never confuses one test with another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClaimApiTest {

	/** Made-up files with just enough bytes to start like the real thing. */
	private static final byte[] PDF = "%PDF-1.7\n% made-up receipt for tests\n%%EOF\n".getBytes(US_ASCII);

	private static final byte[] PNG = concat(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A },
			"made-up photo for tests".getBytes(US_ASCII));

	/** Starts like a Windows program, whatever its name says. */
	private static final byte[] PROGRAM = "MZ\u0090 not really a program".getBytes(UTF_8);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private TestData data;

	private SignedInUser aisyah;
	private SignedInUser daniel;
	private SignedInUser farid;
	private SignedInUser meiLing;

	private final LocalDate today = LocalDate.now(MalaysianTime.ZONE);

	@BeforeEach
	void peopleExist() {
		data = new TestData(jdbc);
		aisyah = signedIn(data.customer(), Role.CUSTOMER);
		daniel = signedIn(data.customer(), Role.CUSTOMER);
		farid = signedIn(data.agent(), Role.AGENT);
		meiLing = signedIn(data.agent(), Role.AGENT);
	}

	@Test
	void theDemoStoryOverHttp() {
		String id = submitAisyahsClaim();

		MvcTestResult result = post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");
		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(read(result, "$.status")).isEqualTo("UNDER_REVIEW");
		assertThat(read(result, "$.assignedAgentId")).isEqualTo(farid.id().toString());

		result = post("/api/claims/" + id + "/request-info", farid, """
				{"version": 1, "message": "Please upload the airline's delay report."}""");
		assertThat(read(result, "$.status")).isEqualTo("MORE_INFO_NEEDED");

		result = respond(id, aisyah, "{\"version\": 2, \"note\": \"Here is the delay report.\"}",
				file("delay-report.pdf", PDF));
		assertThat(read(result, "$.status")).isEqualTo("UNDER_REVIEW");

		result = post("/api/claims/" + id + "/approve", farid, """
				{"version": 3, "amount": 400.00, "reason": "Receipts total RM 400."}""");
		assertThat(result).hasStatus(HttpStatus.OK);

		MvcTestResult page = get("/api/claims/" + id, aisyah);
		assertThat(page).hasStatus(HttpStatus.OK);
		assertThat(read(page, "$.status")).isEqualTo("APPROVED");
		assertThat(new BigDecimal(read(page, "$.approvedAmount").toString())).isEqualByComparingTo("400");
		assertThat(read(page, "$.decisionReason")).isEqualTo("Receipts total RM 400.");
		assertThat(read(page, "$.version")).isEqualTo(4);
		assertThat(strings(page, "$.documents[*].fileName")).containsExactly("receipt.pdf", "delay-report.pdf");
		assertThat(strings(page, "$.timeline[*].toStatus"))
			.containsExactly("SUBMITTED", "UNDER_REVIEW", "MORE_INFO_NEEDED", "UNDER_REVIEW", "APPROVED");
	}

	@Test
	void submittingAnswers201WithTheClaimsAddress() {
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)), file("receipt.pdf", PDF));

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
		MvcTestResult result = get("/api/claims/" + UUID.randomUUID(), farid);

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(read(result, "$.detail")).isEqualTo("Claim not found.");
	}

	@Test
	void anIncompleteRequestAnswers400() {
		MvcTestResult result = submit(aisyah, """
				{"claimType": "BAGGAGE_DELAYED", "incidentDate": "%s", "amountClaimed": 450.00}"""
			.formatted(today.minusDays(10)), file("receipt.pdf", PDF));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void customersSeeOnlyTheirOwnClaims() {
		String id = submitAisyahsClaim();

		assertThat(idsIn(get("/api/claims", aisyah))).contains(id);
		assertThat(idsIn(get("/api/claims", daniel))).doesNotContain(id);
	}

	@Test
	void someoneElsesClaimLooksExactlyLikeAClaimThatDoesntExist() {
		String id = submitAisyahsClaim();

		MvcTestResult opened = get("/api/claims/" + id, daniel);
		MvcTestResult responded = respond(id, daniel, "{\"version\": 0, \"note\": \"Not my claim.\"}");

		assertThat(opened).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(read(opened, "$.detail")).isEqualTo("Claim not found.");
		assertThat(responded).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void anyAgentCanOpenAnyClaim() {
		String id = submitAisyahsClaim();

		assertThat(get("/api/claims/" + id, meiLing)).hasStatus(HttpStatus.OK);
	}

	@Test
	void aClaimMovesBetweenQueueTabsAsItIsWorked() {
		String id = submitAisyahsClaim();
		assertThat(idsIn(queue("submitted", farid))).contains(id);
		assertThat(idsIn(get("/api/claims", farid))).as("Submitted is the default tab").contains(id);

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

	@Test
	void aCustomerCantDoAnAgentsJob() {
		String id = submitAisyahsClaim();

		MvcTestResult result = post("/api/claims/" + id + "/start-review", aisyah, "{\"version\": 0}");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(read(result, "$.detail")).isEqualTo("You don't have access to that.");
		assertThat(queue("submitted", aisyah)).as("customers have no queue").hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void anAgentCantSubmitOrRespond() {
		String id = submitAisyahsClaim();

		assertThat(submit(farid, submitBody(travelPlusFor(aisyah)), file("receipt.pdf", PDF)))
			.hasStatus(HttpStatus.FORBIDDEN);
		assertThat(respond(id, farid, "{\"version\": 0, \"note\": \"Pretending to be Aisyah.\"}"))
			.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void withoutSigningInEveryClaimsEndpointAnswers401() {
		MvcTestResult result = mvc.get().uri("/api/claims").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(read(result, "$.detail")).isEqualTo("Please sign in.");
	}

	@Test
	void aChangeWithoutTheCsrfTokenIsRefused() {
		MvcTestResult result = mvc.post()
			.uri("/api/claims")
			.multipart()
			.file(json("claim", submitBody(travelPlusFor(aisyah))))
			.file(file("receipt.pdf", PDF))
			.with(user(aisyah))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(idsIn(get("/api/claims", aisyah))).isEmpty();
	}

	// Documents

	@Test
	void aClaimArrivesWithItsDocumentsTypedByTheirContent() {
		// The browser says both are PDFs. Only the first really is.
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)),
				new MockMultipartFile("files", "receipt.pdf", "application/pdf", PDF),
				new MockMultipartFile("files", "bag-tag.png", "application/pdf", PNG));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(strings(result, "$.documents[*].fileName")).containsExactly("bag-tag.png", "receipt.pdf");
		assertThat(strings(result, "$.documents[*].contentType")).containsExactly("image/png", "application/pdf");
		assertThat(read(result, "$.documents[1].sizeBytes")).isEqualTo(PDF.length);
		assertThat((String) read(result, "$.documents[1].sha256")).hasSize(64);
	}

	@Test
	void aClaimWithoutDocumentsIsRefused() {
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)));

		assertThat(result).hasStatus(422);
		assertThat(read(result, "$.detail"))
			.isEqualTo("Attach at least one document, such as a receipt or a letter from the airline.");
	}

	@Test
	void aFileIsJudgedByItsContentNotItsName() {
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)),
				new MockMultipartFile("files", "receipt.pdf", "application/pdf", PROGRAM));

		assertThat(result).hasStatus(422);
		assertThat(read(result, "$.title")).isEqualTo("File not accepted");
		assertThat(read(result, "$.detail")).isEqualTo("receipt.pdf isn't a PDF, JPEG or PNG.");
		assertThat(idsIn(get("/api/claims", aisyah))).as("no claim without its files").isEmpty();
	}

	@Test
	void fileNamesAreCleanedBeforeTheyAreStored() {
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)),
				file("..\\..\\My receipt (1).pdf", PDF));

		assertThat(strings(result, "$.documents[*].fileName")).containsExactly("My_receipt__1_.pdf");
	}

	@Test
	void moreThanFiveFilesAtOnceAreRefused() {
		MockMultipartFile[] six = IntStream.rangeClosed(1, 6)
			.mapToObj(n -> file("receipt-" + n + ".pdf", PDF))
			.toArray(MockMultipartFile[]::new);

		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)), six);

		assertThat(result).hasStatus(422);
		assertThat(read(result, "$.detail")).isEqualTo("Attach up to 5 files at a time.");
	}

	@Test
	void aClaimHoldsAtMostFifteenDocuments() {
		String id = (String) read(submit(aisyah, submitBody(travelPlusFor(aisyah)), fivePdfs()), "$.id");
		post("/api/claims/" + id + "/start-review", farid, "{\"version\": 0}");
		long version = 1;
		for (int round = 0; round < 2; round++) {
			post("/api/claims/" + id + "/request-info", farid,
					"{\"version\": " + version + ", \"message\": \"One more set, please.\"}");
			assertThat(respond(id, aisyah, "{\"version\": " + (version + 1) + "}", fivePdfs()))
				.hasStatus(HttpStatus.OK);
			version += 2;
		}
		post("/api/claims/" + id + "/request-info", farid,
				"{\"version\": " + version + ", \"message\": \"And one more, please.\"}");

		MvcTestResult sixteenth = respond(id, aisyah, "{\"version\": " + (version + 1) + "}",
				file("one-too-many.pdf", PDF));

		assertThat(sixteenth).hasStatus(422);
		assertThat(read(sixteenth, "$.detail")).isEqualTo("A claim can have at most 15 documents.");
		assertThat(strings(get("/api/claims/" + id, aisyah), "$.documents[*].id")).hasSize(15);
		assertThat(read(get("/api/claims/" + id, aisyah), "$.status")).as("the response was rolled back")
			.isEqualTo("MORE_INFO_NEEDED");
	}

	@Test
	void aDownloadLinkFetchesTheFileReadOnly() throws IOException, InterruptedException {
		MvcTestResult claim = submit(aisyah, submitBody(travelPlusFor(aisyah)), file("receipt.pdf", PDF));
		String id = (String) read(claim, "$.id");
		String documentId = (String) read(claim, "$.documents[0].id");

		MvcTestResult link = get("/api/claims/" + id + "/documents/" + documentId + "/link", aisyah);

		assertThat(link).hasStatus(HttpStatus.OK);
		String url = (String) read(link, "$.url");
		assertThat(url).contains("sp=r&").contains("rscd=attachment");
		HttpResponse<byte[]> download = HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
		assertThat(download.statusCode()).isEqualTo(200);
		assertThat(download.body()).isEqualTo(PDF);
	}

	@Test
	void someoneElsesDocumentLinkAnswers404() {
		MvcTestResult claim = submit(aisyah, submitBody(travelPlusFor(aisyah)), file("receipt.pdf", PDF));
		String id = (String) read(claim, "$.id");
		String documentId = (String) read(claim, "$.documents[0].id");
		String otherClaim = submitAisyahsClaim();

		assertThat(get("/api/claims/" + id + "/documents/" + documentId + "/link", daniel))
			.hasStatus(HttpStatus.NOT_FOUND);
		MvcTestResult wrongClaim = get("/api/claims/" + otherClaim + "/documents/" + documentId + "/link", aisyah);
		assertThat(wrongClaim).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(read(wrongClaim, "$.detail")).isEqualTo("Document not found.");
		assertThat(get("/api/claims/" + id + "/documents/" + documentId + "/link", farid)).hasStatus(HttpStatus.OK);
	}

	// Helpers

	private static SignedInUser signedIn(UUID id, Role role) {
		return new SignedInUser(id, id + "@example.com", "not-for-sign-in", "Test user", role);
	}

	private String submitAisyahsClaim() {
		MvcTestResult result = submit(aisyah, submitBody(travelPlusFor(aisyah)), file("receipt.pdf", PDF));
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return (String) read(result, "$.id");
	}

	private String submitBody(UUID policy) {
		return """
				{"policyId": "%s", "claimType": "BAGGAGE_DELAYED", "incidentDate": "%s",
				 "amountClaimed": 450.00, "description": "My checked luggage arrived 48 hours late."}"""
			.formatted(policy, today.minusDays(10));
	}

	/** A policy covering 60 days either side of today, so the test works whatever day it runs. */
	private UUID travelPlusFor(SignedInUser customer) {
		return data.travelPlus(customer.id(), today.minusDays(60), today.plusDays(60));
	}

	/** POST /api/claims as the browser sends it: a "claim" JSON part and one "files" part per file. */
	private MvcTestResult submit(SignedInUser who, String claimJson, MockMultipartFile... files) {
		var request = mvc.post().uri("/api/claims").multipart().file(json("claim", claimJson));
		for (MockMultipartFile file : files) {
			request = request.file(file);
		}
		return request.with(user(who)).with(BrowserCsrf.token(mvc)).exchange();
	}

	private MvcTestResult respond(String claimId, SignedInUser who, String responseJson, MockMultipartFile... files) {
		var request = mvc.post()
			.uri("/api/claims/" + claimId + "/respond")
			.multipart()
			.file(json("response", responseJson));
		for (MockMultipartFile file : files) {
			request = request.file(file);
		}
		return request.with(user(who)).with(BrowserCsrf.token(mvc)).exchange();
	}

	private static MockMultipartFile json(String part, String json) {
		return new MockMultipartFile(part, "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(UTF_8));
	}

	private static MockMultipartFile file(String name, byte[] content) {
		return new MockMultipartFile("files", name, "application/octet-stream", content);
	}

	private static MockMultipartFile[] fivePdfs() {
		return IntStream.rangeClosed(1, 5).mapToObj(n -> file("page-" + n + ".pdf", PDF))
			.toArray(MockMultipartFile[]::new);
	}

	private static byte[] concat(byte[] first, byte[] second) {
		byte[] both = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, both, first.length, second.length);
		return both;
	}

	private MvcTestResult post(String path, SignedInUser who, String json) {
		return mvc.post()
			.uri(path)
			.with(user(who))
			.with(BrowserCsrf.token(mvc))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)
			.exchange();
	}

	private MvcTestResult get(String path, SignedInUser who) {
		return mvc.get().uri(path).with(user(who)).exchange();
	}

	private MvcTestResult queue(String tab, SignedInUser who) {
		return get("/api/claims?tab=" + tab, who);
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
