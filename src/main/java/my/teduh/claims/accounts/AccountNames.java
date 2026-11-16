package my.teduh.claims.accounts;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looks the names up in one query, however many ids a page needs. */
@Service
class AccountNames implements NameLookup {

	private final AccountRepository accounts;

	AccountNames(AccountRepository accounts) {
		this.accounts = accounts;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, String> namesOf(Collection<UUID> ids) {
		var wanted = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (wanted.isEmpty()) {
			return Map.of();
		}
		return accounts.findAllById(wanted)
			.stream()
			.collect(Collectors.toMap(Account::getId, Account::getDisplayName));
	}

}
