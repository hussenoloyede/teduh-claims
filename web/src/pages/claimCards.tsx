import type { ClaimSummary } from '../api/claims';
import { ClaimCard } from '../components/ClaimCard/ClaimCard';
import styles from './Page.module.css';

type ClaimCardListProps = {
  claims: ClaimSummary[];
  /** Plan names by policy id (planNames in api/policies.ts). */
  plans: Map<string, string>;
};

// The customer's claims as cards, used on Home (newest three) and Claims (all of them).
export function ClaimCardList({ claims, plans }: ClaimCardListProps) {
  return (
    <ul className={styles.stack}>
      {claims.map((claim) => (
        <li key={claim.id}>
          <ClaimCard
            reference={claim.reference}
            claimType={claim.claimType}
            amountClaimed={claim.amountClaimed}
            approvedAmount={claim.approvedAmount}
            status={claim.status}
            submittedAt={claim.submittedAt}
            policyName={plans.get(claim.policyId)}
            href={`/claims/${claim.id}`}
          />
        </li>
      ))}
    </ul>
  );
}
