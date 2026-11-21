package my.teduh.claims.demo;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.BrowserCsrf;
import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.shared.MalaysianTime;

/**
 * The daily reset over a real database: a new customer with a policy and a claim, then the reset. Afterwards
 * the test runs the startup seeders again, as the next start of the app would, so the tests that run after
 * this one still find the demo accounts and policies.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoResetTest {

	private static final byte[] PDF = "%PDF-1.7\n% made-up receipt for tests\n%%EOF\n".getBytes(US_ASCII);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DemoReset reset;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private TransactionTemplate transaction;

	/** The startup runners in order: this reset, then the demo accounts, then the demo policies. */
	@Autowired
	private List<ApplicationRunner> runners;

	private final LocalDate today = LocalDate.now(MalaysianTime.ZONE);

	@Test
	void clearsTheDayAndRunsOnlyOncePerDay() throws Exception {
		Cookie traveller = newCustomerWithAClaim();
		assertThat(count("select count(*) from users where email like 'traveller-%'")).isPositive();
		assertThat(count("select count(*) from claims")).isPositive();

		jdbc.sql("update demo_reset set last_reset_on = :day where id = 1").param("day", today.minusDays(1)).update();
		assertThat(resetOnce()).isTrue();

		assertThat(count("select count(*) from users where email like 'traveller-%'")).isZero();
		assertThat(count("select count(*) from claims")).isZero();
		assertThat(count("select count(*) from documents")).isZero();
		assertThat(count("select count(*) from notifications")).isZero();
		assertThat(count("select count(*) from policies")).isZero();
		// The new customer's session went with the account.
		assertThat(mvc.get().uri("/api/auth/me").cookie(traveller).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
		// The demo accounts stay.
		assertThat(count("select count(*) from users where email = 'aisyah@example.com'")).isOne();

		// Already done today: a second start the same day changes nothing.
		assertThat(resetOnce()).isFalse();

		// The next start of the app seeds the demo policies again, with fresh cover dates.
		for (ApplicationRunner runner : runners) {
			if (runner.getClass().getPackageName().startsWith("my.teduh.claims")) {
				runner.run(new DefaultApplicationArguments());
			}
		}
		assertThat(count("select count(*) from policies")).isEqualTo(2);
		assertThat(jdbc.sql("select max(cover_end) from policies").query(LocalDate.class).single())
			.isEqualTo(today.plusDays(50));
	}

	private Cookie newCustomerWithAClaim() throws Exception {
		MvcTestResult created = mvc.post().uri("/api/auth/new-customer").with(BrowserCsrf.token(mvc)).exchange();
		assertThat(created).hasStatus(HttpStatus.NO_CONTENT);
		String header = created.getResponse()
			.getHeaders("Set-Cookie")
			.stream()
			.filter(h -> h.startsWith("SESSION="))
			.reduce((first, second) -> second)
			.orElseThrow();
		Cookie session = new Cookie("SESSION", header.substring("SESSION=".length()).split(";", 2)[0]);

		MvcTestResult bought = mvc.post()
			.uri("/api/policies")
			.cookie(session)
			.with(BrowserCsrf.token(mvc))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"plan": "ESSENTIAL", "coverStart": "%s", "coverEnd": "%s"}""".formatted(today, today.plusDays(3)))
			.exchange();
		assertThat(bought).hasStatus(HttpStatus.CREATED);
		String policyId = JsonPath.read(bought.getResponse().getContentAsString(UTF_8), "$.policyId");

		MvcTestResult claim = mvc.post()
			.uri("/api/claims")
			.multipart()
			.file(new MockMultipartFile("claim", "", MediaType.APPLICATION_JSON_VALUE, """
					{"policyId": "%s", "claimType": "FLIGHT_DELAY", "incidentDate": "%s",
					 "amountClaimed": 100.00, "description": "Five hours late."}"""
				.formatted(policyId, today).getBytes(UTF_8)))
			.file(new MockMultipartFile("files", "receipt.pdf", "application/pdf", PDF))
			.cookie(session)
			.with(BrowserCsrf.token(mvc))
			.exchange();
		assertThat(claim).hasStatus(HttpStatus.CREATED);
		return session;
	}

	/** One start of the app: the reset in its own transaction, as DemoReset.run does it. */
	private boolean resetOnce() {
		Boolean reset = transaction.execute(status -> this.reset.resetIfDue(today));
		return Boolean.TRUE.equals(reset);
	}

	private long count(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
