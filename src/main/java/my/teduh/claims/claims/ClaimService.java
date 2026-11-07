package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import my.teduh.claims.policies.PolicyCover;
import my.teduh.claims.policies.PolicyLookup;

/**
 * Everything that happens to a claim goes through here. Each public method is one database transaction:
 * load, check, change, record the timeline line, and commit, or roll all of it back.
 *
 * The rules that need nothing but the claim live in Claim. This class adds the ones that need
 * something else: the policy, today's date, and the version the person was looking at.
 */
@Service
@Transactional
public class ClaimService {

	/** Teduh is a Malaysian insurer, so "today" and the reference year follow Malaysian time, not UTC. */
	static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private final ClaimRepository claims;

	private final ClaimEventRepository events;

	private final PolicyLookup policies;

	private final Clock clock;

	ClaimService(ClaimRepository claims, ClaimEventRepository events, PolicyLookup policies, Clock clock) {
		this.claims = claims;
		this.events = events;
		this.policies = policies;
		this.clock = clock;
	}

	/**
	 * Checks the claim against the customer's policy, gives it the next reference number and saves it
	 * with the first line of its timeline. From week 3 the files arrive in the same request.
	 */
	public Claim submit(NewClaim details) {
		PolicyCover policy = policies.findForCustomer(details.policyId(), details.customerId())
			.orElseThrow(() -> new ClaimRuleException("We couldn't find that policy on your account."));

		LocalDate today = LocalDate.now(clock.withZone(MALAYSIA));
		if (details.incidentDate() != null) {
			if (details.incidentDate().isAfter(today)) {
				throw new ClaimRuleException("The incident date can't be in the future.");
			}
			if (!policy.covers(details.incidentDate())) {
				throw new ClaimRuleException("The incident date must be between " + DAY.format(policy.coverStart())
						+ " and " + DAY.format(policy.coverEnd()) + ", when your policy was active.");
			}
		}
		if (details.claimType() != null && details.amountClaimed() != null) {
			BigDecimal limit = policy.limitFor(details.claimType())
				.orElseThrow(() -> new ClaimRuleException(
						"Your policy doesn't cover " + details.claimType().label().toLowerCase(Locale.ENGLISH) + "."));
			if (details.amountClaimed().compareTo(limit) > 0) {
				throw new ClaimRuleException(
						"Your limit for " + details.claimType().label() + " is " + ringgit(limit) + ".");
			}
		}

		Instant now = clock.instant();
		String reference = ClaimReference.format(now.atZone(MALAYSIA).getYear(), claims.nextReferenceNumber());
		Claim claim = Claim.submit(reference, details, now);
		claims.save(claim);
		events.save(ClaimEvent.submitted(claim));
		return claim;
	}

	public Claim startReview(UUID claimId, long seenVersion, UUID agentId) {
		return act(claimId, seenVersion, claim -> claim.startReview(agentId));
	}

	public Claim requestInfo(UUID claimId, long seenVersion, UUID agentId, String message) {
		return act(claimId, seenVersion, claim -> claim.requestInfo(agentId, message));
	}

	public Claim respond(UUID claimId, long seenVersion, UUID customerId, String note, boolean hasFiles) {
		return act(claimId, seenVersion, claim -> claim.respond(customerId, note, hasFiles));
	}

	public Claim approve(UUID claimId, long seenVersion, UUID agentId, BigDecimal amount, String reason) {
		return act(claimId, seenVersion, claim -> claim.approve(agentId, amount, reason));
	}

	public Claim reject(UUID claimId, long seenVersion, UUID agentId, String reason) {
		return act(claimId, seenVersion, claim -> claim.reject(agentId, reason));
	}

	@Transactional(readOnly = true)
	public List<Claim> claimsOf(UUID customerId) {
		return claims.findByCustomerIdOrderBySubmittedAtDesc(customerId);
	}

	/** One tab of the agent queue. Every tab is sorted oldest first, so the longest wait is at the top. */
	@Transactional(readOnly = true)
	public List<Claim> queue(QueueTab tab, UUID agentId) {
		return switch (tab) {
			case SUBMITTED -> claims.findByStatusOrderBySubmittedAtAsc(ClaimStatus.SUBMITTED);
			case IN_REVIEW_BY_ME -> claims.findByStatusInAndAssignedAgentIdOrderBySubmittedAtAsc(
					List.of(ClaimStatus.UNDER_REVIEW), agentId);
			case WAITING_ON_CUSTOMER -> claims.findByStatusInAndAssignedAgentIdOrderBySubmittedAtAsc(
					List.of(ClaimStatus.MORE_INFO_NEEDED), agentId);
			case DECIDED -> claims.findByStatusInAndAssignedAgentIdOrderBySubmittedAtAsc(
					List.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED), agentId);
		};
	}

	@Transactional(readOnly = true)
	public Claim find(UUID claimId) {
		return claims.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
	}

	@Transactional(readOnly = true)
	public List<ClaimEvent> timeline(UUID claimId) {
		return events.findByClaimIdOrderByOccurredAtAscIdAsc(claimId);
	}

	/**
	 * The shared shape of every action. The version check comes first: if the claim changed since the
	 * person loaded it, they get "reload" (409) rather than a rule error about a status they never saw.
	 * The @Version column then guards the gap between this check and the commit.
	 */
	private Claim act(UUID claimId, long seenVersion, Function<Claim, ClaimTransition> action) {
		Claim claim = claims.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
		if (claim.getVersion() != seenVersion) {
			throw new OptimisticLockingFailureException("This claim changed while you were looking at it.");
		}
		ClaimTransition transition = action.apply(claim);
		events.save(ClaimEvent.of(claim.getId(), transition, clock.instant()));
		// No save(claim) needed: the claim was loaded in this transaction, so Hibernate writes its
		// changes (and bumps the version) when the transaction commits.
		return claim;
	}

	static String ringgit(BigDecimal amount) {
		return String.format(Locale.ROOT, "RM %,.2f", amount);
	}

}
