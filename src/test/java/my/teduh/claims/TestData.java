package my.teduh.claims;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import my.teduh.claims.accounts.Role;

/**
 * Rows that tests need before they can make a claim: the people involved and a policy.
 * Written in plain SQL, so a test of one module doesn't depend on another module's internals.
 * Every insert is "on conflict do nothing", so calling it twice for the same id is harmless.
 */
public final class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public UUID customer() {
		return user(UUID.randomUUID(), Role.CUSTOMER);
	}

	public UUID agent() {
		return user(UUID.randomUUID(), Role.AGENT);
	}

	/** A user who can't sign in: the hash is not a real BCrypt hash. Sign-in tests use the demo accounts. */
	public UUID user(UUID id, Role role) {
		jdbc.update("""
				insert into users (id, email, password_hash, display_name, role)
				values (?, ?, 'not-for-sign-in', 'Test user', ?)
				on conflict (id) do nothing""", id, id + "@example.com", role.name());
		return id;
	}

	public UUID travelPlus(UUID customer, LocalDate coverStart, LocalDate coverEnd) {
		return travelPlus(UUID.randomUUID(), customer, coverStart, coverEnd);
	}

	/** Teduh Travel Plus with the four limits from the scope. */
	public UUID travelPlus(UUID policy, UUID customer, LocalDate coverStart, LocalDate coverEnd) {
		int added = jdbc.update("""
				insert into policies (id, policy_number, customer_id, plan_name, cover_start, cover_end)
				values (?, ?, ?, 'Teduh Travel Plus', ?, ?)
				on conflict (id) do nothing""", policy, "TT-" + policy.toString().substring(0, 8), customer,
				coverStart, coverEnd);
		if (added == 1) {
			String limit = "insert into policy_limits (policy_id, claim_type, limit_amount) values (?, ?, ?)";
			jdbc.update(limit, policy, "FLIGHT_DELAY", new BigDecimal("500.00"));
			jdbc.update(limit, policy, "BAGGAGE_DELAYED", new BigDecimal("1000.00"));
			jdbc.update(limit, policy, "LOST_OR_DAMAGED_BAGGAGE", new BigDecimal("3000.00"));
			jdbc.update(limit, policy, "TRIP_CANCELLATION", new BigDecimal("10000.00"));
		}
		return policy;
	}

}
