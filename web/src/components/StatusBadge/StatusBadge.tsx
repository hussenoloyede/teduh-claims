import { statusLabel, type ClaimStatus } from '../../api/claims';
import styles from './StatusBadge.module.css';

type StatusBadgeProps = {
  status: ClaimStatus;
};

// Shows a claim's status as a coloured pill with the status written out,
// so the colour is never the only signal.
export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={styles.badge} data-status={status}>
      {statusLabel[status]}
    </span>
  );
}
