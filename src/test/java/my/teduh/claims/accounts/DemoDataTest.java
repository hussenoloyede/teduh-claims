package my.teduh.claims.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.policies.PolicyCover;
import my.teduh.claims.policies.PolicyLookup;
import my.teduh.claims.shared.MalaysianTime;

/** What the app has seeded by the time it has started: four demo people, and a policy for each customer. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoDataTest {

	@Autowired
	private AccountRepository accounts;

	@Autowired
	private PasswordEncoder passwords;

	@Autowired
	private PolicyLookup policies;

	@Autowired
	private Clock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ApplicationContext context;

	@Test
	void theDemoAccountsExistWithTheRightRoles() {
		assertThat(accounts.findById(DemoAccounts.AISYAH)).hasValueSatisfying(account -> {
			assertThat(account.getEmail()).isEqualTo("aisyah@example.com");
			assertThat(account.getDisplayName()).isEqualTo("Aisyah Rahman");
			assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
		});
		assertThat(accounts.findById(DemoAccounts.DANIEL)).map(Account::getRole).hasValue(Role.CUSTOMER);
		assertThat(accounts.findById(DemoAccounts.FARID)).map(Account::getRole).hasValue(Role.AGENT);
		assertThat(accounts.findById(DemoAccounts.MEI_LING)).map(Account::getRole).hasValue(Role.AGENT);
	}

	@Test
	void passwordsAreStoredOnlyAsBcryptHashes() {
		String hash = accounts.findById(DemoAccounts.FARID).orElseThrow().getPasswordHash();

		assertThat(hash).startsWith("{bcrypt}$2a$").doesNotContain(DemoAccounts.PASSWORD);
		assertThat(passwords.matches(DemoAccounts.PASSWORD, hash)).isTrue();
		assertThat(passwords.matches("teduh-demo-wrong", hash)).isFalse();
	}

	@Test
	void anEmailIsFoundHoweverItWasTyped() {
		assertThat(accounts.findByEmailAsTyped("  Aisyah@Example.COM ")).map(Account::getId)
			.hasValue(DemoAccounts.AISYAH);
		assertThat(accounts.findByEmailAsTyped("nobody@example.com")).isEmpty();
	}

	@Test
	void eachDemoCustomerHasAPolicyThatCoversToday() {
		assertThat(policies.policiesOf(DemoAccounts.AISYAH)).singleElement().satisfies(policy -> {
			assertThat(policy.planName()).isEqualTo("Teduh Travel Plus");
			assertThat(policy.covers(MalaysianTime.today(clock))).isTrue();
		});
		assertThat(policies.policiesOf(DemoAccounts.DANIEL)).singleElement()
			.extracting(PolicyCover::policyNumber)
			.isEqualTo("TTP-26-005203");
	}

	@Test
	void runningTheSeedersAgainChangesNothing() throws Exception {
		String hashBefore = accounts.findById(DemoAccounts.AISYAH).orElseThrow().getPasswordHash();

		// Bean names come from the class names: DemoAccountSeeder becomes demoAccountSeeder.
		context.getBean("demoAccountSeeder", ApplicationRunner.class).run(new DefaultApplicationArguments());
		context.getBean("demoPolicySeeder", ApplicationRunner.class).run(new DefaultApplicationArguments());

		// A fresh hash would differ even for the same password (BCrypt adds a random salt), so an unchanged
		// hash shows the account was left alone rather than overwritten.
		assertThat(accounts.findById(DemoAccounts.AISYAH).orElseThrow().getPasswordHash()).isEqualTo(hashBefore);
		assertThat(jdbc.queryForObject("select count(*) from policies where customer_id in (?, ?)", Integer.class,
				DemoAccounts.AISYAH, DemoAccounts.DANIEL)).isEqualTo(2);
	}

}
