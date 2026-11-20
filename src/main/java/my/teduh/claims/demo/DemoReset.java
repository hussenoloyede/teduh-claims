package my.teduh.claims.demo;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import my.teduh.claims.documents.DocumentStore;
import my.teduh.claims.shared.MalaysianTime;

/**
 * Puts the public demo back to its starting state once a day, so every visitor finds the same story and
 * nobody's test claims pile up. It runs when the app starts, before the demo seeders (order 1 and 2), and
 * only if it hasn't already run today in Malaysia. The live app sleeps when nobody uses it and starts again
 * on the next visit, so in practice the first visit of each day gets a fresh demo.
 *
 * It clears every claim with its history, documents and notifications, the event log, every policy, and
 * the accounts made with "Try as a new customer" (and their sessions). The four demo accounts stay. The
 * seeders then create Aisyah's and Daniel's policies again, with cover dates around the new today.
 *
 * This is the one class that knows every module's tables, on purpose: deleting in foreign-key order is
 * easiest to check when it is written out in one place. It only runs SQL; it never uses another module's
 * classes, apart from the documents module's public DocumentStore, which also removes the stored files.
 */
@Component
@Order(0)
class DemoReset implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoReset.class);

	private final JdbcClient jdbc;

	private final DocumentStore documents;

	private final TransactionTemplate transaction;

	private final Clock clock;

	DemoReset(JdbcClient jdbc, DocumentStore documents, TransactionTemplate transaction, Clock clock) {
		this.jdbc = jdbc;
		this.documents = documents;
		this.transaction = transaction;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		transaction.executeWithoutResult(status -> resetIfDue(MalaysianTime.today(clock)));
	}

	/** Resets unless it already happened today. Returns whether it reset. Package-private for the test. */
	boolean resetIfDue(LocalDate today) {
		// "for update" locks the row, so two copies of the app starting together can't both reset.
		LocalDate last = jdbc.sql("select last_reset_on from demo_reset where id = 1 for update")
			.query(LocalDate.class)
			.single();
		if (!last.isBefore(today)) {
			return false;
		}
		int files = documents.deleteAll();
		jdbc.sql("delete from notifications").update();
		jdbc.sql("delete from claim_events").update();
		int claims = jdbc.sql("delete from claims").update();
		jdbc.sql("delete from event_publication").update();
		jdbc.sql("delete from policy_limits").update();
		jdbc.sql("delete from policies").update();
		// Signs out every "new customer" before their account goes. Their session's attributes go with it
		// (on delete cascade).
		jdbc.sql("delete from spring_session where principal_name like 'traveller-%'").update();
		int travellers = jdbc.sql("delete from users where email like 'traveller-%'").update();
		// References start again from CLM-....-000001 and TTP-..-010000, as on a fresh install.
		jdbc.sql("alter sequence claim_reference_seq restart").update();
		jdbc.sql("alter sequence policy_number_seq restart").update();
		jdbc.sql("update demo_reset set last_reset_on = :today where id = 1").param("today", today).update();
		log.info("Demo reset for {}: removed {} claims, {} documents and {} new demo customers.", today, claims,
				files, travellers);
		return true;
	}

}
