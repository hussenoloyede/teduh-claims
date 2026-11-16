package my.teduh.claims.accounts;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * People's display names for other modules, so a claim page can say "Requested by Farid Hassan" rather than
 * show an id. Only the name leaves this module: never the email, the role or the password hash.
 */
public interface NameLookup {

	/** The display name of each id that belongs to an account. Unknown ids are simply left out. */
	Map<UUID, String> namesOf(Collection<UUID> ids);

}
