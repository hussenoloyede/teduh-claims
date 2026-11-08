package my.teduh.claims.accounts;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccountRepository extends JpaRepository<Account, UUID> {

	/** Spring Data writes this query from the method name: select ... from users where email = ? */
	Optional<Account> findByEmail(String email);

	/** What sign-in will call: the email as the person typed it, spaces and capitals included. */
	default Optional<Account> findByEmailAsTyped(String email) {
		return findByEmail(Account.normalise(email));
	}

}
