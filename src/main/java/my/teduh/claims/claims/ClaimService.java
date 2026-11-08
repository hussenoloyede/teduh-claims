package my.teduh.claims.claims;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import my.teduh.claims.documents.DocumentStore;
import my.teduh.claims.documents.DownloadLink;
import my.teduh.claims.documents.StoredDocument;
import my.teduh.claims.documents.UploadedFile;
import my.teduh.claims.policies.PolicyCover;
import my.teduh.claims.policies.PolicyLookup;
import my.teduh.claims.shared.MalaysianTime;

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

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private final ClaimRepository claims;

	private final ClaimEventRepository events;

	private final PolicyLookup policies;

	private final DocumentStore documents;

	private final Clock clock;

	ClaimService(ClaimRepository claims, ClaimEventRepository events, PolicyLookup policies, DocumentStore documents,
			Clock clock) {
		this.claims = claims;
		this.events = events;
		this.policies = policies;
		this.documents = documents;
		this.clock = clock;
	}

	/**
	 * Checks the claim against the customer's policy, gives it the next reference number and saves it
	 * with the first line of its timeline and its documents. All in one transaction: if a file is refused,
	 * the claim isn't created either.
	 */
	public Claim submit(NewClaim details, List<UploadedFile> files) {
		PolicyCover policy = policies.findForCustomer(details.policyId(), details.customerId())
			.orElseThrow(() -> new ClaimRuleException("We couldn't find that policy on your account."));

		LocalDate today = MalaysianTime.today(clock);
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

		if (files.isEmpty()) {
			throw new ClaimRuleException("Attach at least one document, such as a receipt or a letter from the airline.");
		}

		Instant now = clock.instant();
		String reference = ClaimReference.format(now.atZone(MalaysianTime.ZONE).getYear(), claims.nextReferenceNumber());
		Claim claim = Claim.submit(reference, details, now);
		claims.save(claim);
		events.save(ClaimEvent.submitted(claim));
		documents.store(claim.getId(), details.customerId(), files);
		return claim;
	}

	public Claim startReview(UUID claimId, long seenVersion, UUID agentId) {
		return act(claimId, seenVersion, claim -> claim.startReview(agentId));
	}

	public Claim requestInfo(UUID claimId, long seenVersion, UUID agentId, String message) {
		return act(claimId, seenVersion, claim -> claim.requestInfo(agentId, message));
	}

	/**
	 * Someone else's claim answers 404 before any rule is checked, so the response reveals nothing about it.
	 * The files are stored only once the claim has accepted the response, and in the same transaction.
	 */
	public Claim respond(UUID claimId, long seenVersion, UUID customerId, String note, List<UploadedFile> files) {
		findForCustomer(claimId, customerId);
		Claim claim = act(claimId, seenVersion, c -> c.respond(customerId, note, !files.isEmpty()));
		if (!files.isEmpty()) {
			documents.store(claimId, customerId, files);
		}
		return claim;
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

	/**
	 * A customer's own claim. Someone else's claim throws the same ClaimNotFoundException as a made-up id:
	 * answering 403 would confirm that a claim with that id exists.
	 */
	@Transactional(readOnly = true)
	public Claim findForCustomer(UUID claimId, UUID customerId) {
		return claims.findByIdAndCustomerId(claimId, customerId).orElseThrow(() -> new ClaimNotFoundException(claimId));
	}

	@Transactional(readOnly = true)
	public List<ClaimEvent> timeline(UUID claimId) {
		return events.findByClaimIdOrderByOccurredAtAscIdAsc(claimId);
	}

	@Transactional(readOnly = true)
	public List<StoredDocument> documents(UUID claimId) {
		return documents.documentsOf(claimId);
	}

	/** Callers check the person may see the claim first; this only checks the document is the claim's. */
	@Transactional(readOnly = true)
	public Optional<DownloadLink> linkFor(UUID claimId, UUID documentId) {
		return documents.linkFor(claimId, documentId);
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
