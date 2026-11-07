package my.teduh.claims.policies;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

/** A travel policy as stored. Package-private: other modules only ever see a PolicyCover. */
@Entity
@Table(name = "policies")
class Policy {

	@Id
	private UUID id;

	private String policyNumber;

	private UUID customerId;

	private String planName;

	private LocalDate coverStart;

	private LocalDate coverEnd;

	/** One row in policy_limits per claim type the policy covers. Loaded with the policy, since it's tiny. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "policy_limits", joinColumns = @JoinColumn(name = "policy_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "claim_type")
	@Column(name = "limit_amount")
	private Map<ClaimType, BigDecimal> limits = new HashMap<>();

	protected Policy() {
	}

	PolicyCover toCover() {
		return new PolicyCover(id, customerId, policyNumber, planName, coverStart, coverEnd, limits);
	}

}
