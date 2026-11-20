package my.teduh.claims.accounts;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * "Try as a new customer": a fresh customer with no policy and no claims, so a visitor can go through the
 * welcome screens and choose a plan. Nobody types a name, email or password, so no real personal data
 * ever reaches the demo. The demo reset clears these accounts.
 */
@Service
class NewDemoCustomers {

	/** Every new demo customer's email starts with this, so they can be counted and cleared. */
	static final String EMAIL_PREFIX = "traveller-";

	/** A public demo shouldn't let one person fill the database. The daily reset brings the count back to 0. */
	static final int LIMIT = 300;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AccountRepository accounts;

	private final PasswordEncoder passwords;

	NewDemoCustomers(AccountRepository accounts, PasswordEncoder passwords) {
		this.accounts = accounts;
		this.passwords = passwords;
	}

	@Transactional
	SignedInUser create() {
		if (accounts.countByEmailStartingWith(EMAIL_PREFIX) >= LIMIT) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"The demo has made as many new customers as it can today. Try as Aisyah instead.");
		}
		UUID id = UUID.randomUUID();
		String email = EMAIL_PREFIX + id.toString().substring(0, 8) + "@example.com";
		// A long random password that nobody ever sees: the account is only reachable through this session.
		byte[] secret = new byte[32];
		RANDOM.nextBytes(secret);
		String hash = passwords.encode(Base64.getEncoder().encodeToString(secret));
		Account account = accounts.save(new Account(id, email, hash, "Demo traveller", Role.CUSTOMER));
		SignedInUser user = SignedInUser.of(account);
		user.eraseCredentials();
		return user;
	}

}
