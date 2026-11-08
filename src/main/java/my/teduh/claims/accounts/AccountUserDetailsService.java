package my.teduh.claims.accounts;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * How Spring Security finds the account behind the email typed at sign-in. It then compares the password
 * with the stored BCrypt hash itself.
 *
 * When no account has that email, Spring Security still runs a BCrypt check against a dummy hash, so a
 * wrong email takes as long as a wrong password. Otherwise the response time would tell an attacker
 * which emails have accounts.
 */
@Service
class AccountUserDetailsService implements UserDetailsService {

	private final AccountRepository accounts;

	AccountUserDetailsService(AccountRepository accounts) {
		this.accounts = accounts;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return accounts.findByEmailAsTyped(email)
			.map(SignedInUser::of)
			.orElseThrow(() -> new UsernameNotFoundException("No account with that email"));
	}

}
