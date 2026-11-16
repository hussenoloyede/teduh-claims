import { Luggage } from 'lucide-react';
import { Link } from 'react-router';
import { claimTypeLabel, type ClaimStatus, type ClaimType } from '../../api/claims';
import { formatDate, formatMoney } from '../../lib/format';
import { StatusBadge } from '../StatusBadge/StatusBadge';
import styles from './ClaimCard.module.css';

type ClaimCardProps = {
  reference: string;
  claimType: ClaimType;
  amountClaimed: number;
  /** Shown instead of the claimed amount once a claim is approved. */
  approvedAmount?: number | null;
  status: ClaimStatus;
  /** When the claim was submitted, as the API sends it (UTC). */
  submittedAt: string;
  /** The plan the claim is under. Left out of the line when not known yet. */
  policyName?: string;
  /** The claim page this card opens. */
  href: string;
};

// One claim in the customer's list. The whole card is the link to the claim.
export function ClaimCard({
  reference,
  claimType,
  amountClaimed,
  approvedAmount,
  status,
  submittedAt,
  policyName,
  href,
}: ClaimCardProps) {
  const approved = status === 'APPROVED' && approvedAmount != null;
  return (
    <Link className={styles.card} to={href}>
      <div className={styles.head}>
        <span className={styles.icon} aria-hidden="true">
          <Luggage size={16} />
        </span>
        <div className={styles.title}>
          <span className={styles.reference}>{reference}</span>
          <span className={styles.meta}>
            {policyName ? `${policyName} · ` : ''}
            {formatDate(submittedAt)}
          </span>
        </div>
        <StatusBadge status={status} />
      </div>
      <dl className={styles.facts}>
        <div>
          <dt>Incident reason</dt>
          <dd className={styles.value}>{claimTypeLabel[claimType]}</dd>
        </div>
        <div className={styles.amount}>
          <dt>{approved ? 'Approved amount' : 'Claimed amount'}</dt>
          <dd className={styles.money}>{formatMoney(approved ? approvedAmount : amountClaimed)}</dd>
        </div>
      </dl>
    </Link>
  );
}
