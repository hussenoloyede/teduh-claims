package my.teduh.claims.accounts;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The person behind the current request. Spring Security keeps it in the session after sign-in, and
 * controllers receive it with @AuthenticationPrincipal. This is how other modules learn who is acting,
 * without ever touching the Account entity or the users table.
 *
 * The password hash is only here so Spring Security can check it during sign-in. It is wiped straight
 * after (eraseCredentials), so the copy kept in the session never holds it.
 */
public final class SignedInUser implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID id;

	private final String email;

	private String passwordHash;

	private final String displayName;

	private final Role role;

	public SignedInUser(UUID id, String email, String passwordHash, String displayName, Role role) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	static SignedInUser of(Account account) {
		return new SignedInUser(account.getId(), account.getEmail(), account.getPasswordHash(),
				account.getDisplayName(), account.getRole());
	}

	public UUID id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String displayName() {
		return displayName;
	}

	public Role role() {
		return role;
	}

	public boolean isAgent() {
		return role == Role.AGENT;
	}

	/** Spring Security's name for a role is "ROLE_" plus the role, which is what hasRole('AGENT') looks for. */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}
