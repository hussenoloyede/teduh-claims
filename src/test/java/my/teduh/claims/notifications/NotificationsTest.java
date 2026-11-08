package my.teduh.claims.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.Duration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.BrowserCsrf;
import my.teduh.claims.TestData;
import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.accounts.Role;
import my.teduh.claims.accounts.SignedInUser;
import my.teduh.claims.claims.Claim;
import my.teduh.claims.claims.ClaimService;
import my.teduh.claims.claims.NewClaim;
import my.teduh.claims.documents.UploadedFile;
import my.teduh.claims.policies.ClaimType;
import my.teduh.claims.shared.MalaysianTime;

/**
 * Claim changes made through ClaimService arrive as notifications through the event, the background listener
 * and the /api/notifications endpoints, the whole way a real change travels. The listener runs after the
 * claim's transaction commits, on another thread, so each check waits for the notification to appear.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationsTest {

	private static final List<UploadedFile> RECEIPT = List
		.of(new UploadedFile("receipt.pdf", "%PDF-1.7\n% made-up receipt for tests\n%%EOF\n".getBytes()));

	private static final Duration PATIENCE = Duration.ofSeconds(10);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ClaimService claims;

	@Autowired
	private JdbcTemplate jdbc;

	private SignedInUser aisyah;
	private SignedInUser daniel;
	private SignedInUser farid;

	@BeforeEach
	void peopleExist() {
		TestData data = new TestData(jdbc);
		aisyah = signedIn(data.customer(), Role.CUSTOMER);
		daniel = signedIn(data.customer(), Role.CUSTOMER);
		farid = signedIn(data.agent(), Role.AGENT);
	}

	@Test
	void theCustomerHearsAboutEveryStepAndTheAgentHearsTheResponse() {
		Claim claim = submitAisyahsClaim();
		String ref = claim.getReference();
		awaitUnread(aisyah, 1);
		assertThat(messages(aisyah)).containsExactly(
				"We've received your claim " + ref + ". An agent will start reviewing it soon.");

		claim = claims.startReview(claim.getId(), claim.getVersion(), farid.id());
		awaitUnread(aisyah, 2);

		claim = claims.requestInfo(claim.getId(), claim.getVersion(), farid.id(), "Please send the delay report.");
		awaitUnread(aisyah, 3);

		claim = claims.respond(claim.getId(), claim.getVersion(), aisyah.id(), "Here it is.", RECEIPT);
		awaitUnread(farid, 1);
		assertThat(messages(farid)).containsExactly("The customer responded on claim " + ref + ".");

		claims.approve(claim.getId(), claim.getVersion(), farid.id(), new BigDecimal("400.00"), "Receipts total RM 400.");
		awaitUnread(aisyah, 4);
		assertThat(messages(aisyah)).as("newest first").containsExactly(
				"Your claim " + ref + " was approved. See the claim page for the amount.",
				"Your agent needs more information for claim " + ref + ". See the claim page for what to send.",
				"An agent has started reviewing your claim " + ref + ".",
				"We've received your claim " + ref + ". An agent will start reviewing it soon.");
	}

	@Test
	void readingOneThenAllClearsTheCount() {
		Claim claim = submitAisyahsClaim();
		claims.startReview(claim.getId(), claim.getVersion(), farid.id());
		awaitUnread(aisyah, 2);
		String newest = (String) read(inbox(aisyah), "$.notifications[0].id");

		assertThat(post("/api/notifications/" + newest + "/read", aisyah)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(read(inbox(aisyah), "$.unreadCount")).isEqualTo(1);
		assertThat(read(inbox(aisyah), "$.notifications[0].read")).isEqualTo(true);

		assertThat(post("/api/notifications/read-all", aisyah)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(read(inbox(aisyah), "$.unreadCount")).isEqualTo(0);
	}

	@Test
	void everyoneSeesOnlyTheirOwnNotifications() {
		submitAisyahsClaim();
		awaitUnread(aisyah, 1);
		String aisyahs = (String) read(inbox(aisyah), "$.notifications[0].id");

		assertThat(read(inbox(daniel), "$.unreadCount")).isEqualTo(0);
		assertThat(post("/api/notifications/" + aisyahs + "/read", daniel)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(read(inbox(aisyah), "$.unreadCount")).as("still unread").isEqualTo(1);
	}

	@Test
	void aDeliveredEventLeavesNothingBehindInTheOutbox() {
		Claim claim = submitAisyahsClaim();
		awaitUnread(aisyah, 1);

		// completion-mode: delete. Once the listener has finished, the stored event is removed.
		await().atMost(PATIENCE).untilAsserted(() -> assertThat(jdbc.queryForObject(
				"select count(*) from event_publication where serialized_event like ?", Integer.class,
				"%" + claim.getId() + "%")).isZero());
	}

	@Test
	void aChangeThatIsRolledBackSendsNothing() {
		LocalDate today = MalaysianTime.today(java.time.Clock.systemUTC());
		UUID policy = new TestData(jdbc).travelPlus(aisyah.id(), today.minusDays(60), today.plusDays(60));
		List<UploadedFile> notAPdf = List.of(new UploadedFile("receipt.pdf", "MZ not a pdf".getBytes()));

		try {
			claims.submit(baggageDelayed(policy, today), notAPdf);
		}
		catch (RuntimeException expected) {
			// The file is refused, so the claim and its event are rolled back together.
		}

		// Give a wrongly delivered event the same time a real one gets, then check nothing arrived.
		await().during(Duration.ofSeconds(2)).atMost(PATIENCE)
			.untilAsserted(() -> assertThat(read(inbox(aisyah), "$.unreadCount")).isEqualTo(0));
	}

	// Helpers

	private static SignedInUser signedIn(UUID id, Role role) {
		return new SignedInUser(id, id + "@example.com", "not-for-sign-in", "Test user", role);
	}

	private Claim submitAisyahsClaim() {
		LocalDate today = MalaysianTime.today(java.time.Clock.systemUTC());
		UUID policy = new TestData(jdbc).travelPlus(aisyah.id(), today.minusDays(60), today.plusDays(60));
		return claims.submit(baggageDelayed(policy, today), RECEIPT);
	}

	private NewClaim baggageDelayed(UUID policy, LocalDate today) {
		return new NewClaim(aisyah.id(), policy, ClaimType.BAGGAGE_DELAYED, today.minusDays(10),
				new BigDecimal("450.00"), "My checked luggage arrived 48 hours late.");
	}

	private void awaitUnread(SignedInUser who, int count) {
		await().atMost(PATIENCE).untilAsserted(() -> assertThat(read(inbox(who), "$.unreadCount")).isEqualTo(count));
	}

	private MvcTestResult inbox(SignedInUser who) {
		MvcTestResult result = mvc.get().uri("/api/notifications").with(user(who)).exchange();
		assertThat(result).hasStatus(HttpStatus.OK);
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<String> messages(SignedInUser who) {
		return (List<String>) read(inbox(who), "$.notifications[*].message");
	}

	private MvcTestResult post(String path, SignedInUser who) {
		return mvc.post().uri(path).with(user(who)).with(BrowserCsrf.token(mvc)).exchange();
	}

	private static Object read(MvcTestResult result, String path) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), path);
		}
		catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

}
