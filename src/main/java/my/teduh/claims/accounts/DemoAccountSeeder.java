package my.teduh.claims.accounts;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at startup, after Flyway has created the tables, and adds any demo account that is missing.
 * Running it again changes nothing. Order 1, because the demo policies (order 2) point at these accounts.
 */
@Component
@Order(1)
class DemoAccountSeeder implements ApplicationRunner {

	private final AccountRepository accounts;

	private final PasswordEncoder passwords;

	DemoAccountSeeder(AccountRepository accounts, PasswordEncoder passwords) {
		this.accounts = accounts;
		this.passwords = passwords;
	}

	@Override
	public void run(ApplicationArguments args) {
		String hash = passwords.encode(DemoAccounts.PASSWORD);
		List.of(new Account(DemoAccounts.AISYAH, "aisyah@example.com", hash, "Aisyah Rahman", Role.CUSTOMER),
				new Account(DemoAccounts.DANIEL, "daniel@example.com", hash, "Daniel Tan", Role.CUSTOMER),
				new Account(DemoAccounts.FARID, "farid@example.com", hash, "Farid Hassan", Role.AGENT),
				new Account(DemoAccounts.MEI_LING, "meiling@example.com", hash, "Mei Ling Wong", Role.AGENT))
			.stream()
			.filter(account -> !accounts.existsById(account.getId()))
			.forEach(accounts::save);
	}

}
