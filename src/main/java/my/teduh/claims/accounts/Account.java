package my.teduh.claims.accounts;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Someone who can sign in. Called Account rather than User so it never gets mixed up with Spring Security's
 * own User class. Package-private: other modules learn about the signed-in person through what this module
 * makes public, never through the entity.
 */
@Entity
@Table(name = "users")
class Account {

	@Id
	private UUID id;

	/** Always stored in lower case, so "Aisyah@Example.com" and "aisyah@example.com" are the same person. */
	private String email;

	/** A BCrypt hash with its "{bcrypt}" prefix. The password itself is never stored. */
	private String passwordHash;

	private String displayName;

	@Enumerated(EnumType.STRING)
	private Role role;

	protected Account() {
	}

	Account(UUID id, String email, String passwordHash, String displayName, Role role) {
		this.id = id;
		this.email = normalise(email);
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	static String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	UUID getId() {
		return id;
	}

	String getEmail() {
		return email;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	String getDisplayName() {
		return displayName;
	}

	Role getRole() {
		return role;
	}

}
