package my.teduh.claims.accounts;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class AccountsConfiguration {

	/**
	 * Hashes new passwords with BCrypt and stores them as "{bcrypt}$2a$10$...". The prefix records which
	 * algorithm made the hash, so a stronger one can be introduced later without breaking existing accounts.
	 * BCrypt is slow on purpose (about a tenth of a second per hash), which makes guessing passwords from a
	 * stolen table expensive.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
