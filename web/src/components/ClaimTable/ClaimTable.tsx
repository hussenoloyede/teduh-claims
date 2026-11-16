import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router';
import type { ClaimStatus } from '../../api/claims';
import { formatMoney } from '../../lib/format';
import { Avatar } from '../Avatar/Avatar';
import { StatusBadge } from '../StatusBadge/StatusBadge';
import styles from './ClaimTable.module.css';

export type ClaimTableRow = {
  id: string;
  reference: string;
  customerName: string;
  claimType: string;
  /** What the amount column shows: the approved amount once approved, otherwise what was claimed. */
  amount: number;
  /** The claimed amount, shown small under an approval for less. */
  claimedAmount?: number;
  status: ClaimStatus;
  /** The time column's text for this row, such as "3 days" or "7 Nov 2026". */
  when: string;
  href: string;
  actionLabel: string;
};

type ClaimTableProps = {
  /** Read by screen readers as the table's name, such as "Claims waiting for review". */
  caption: string;
  /** The time column's heading: "Waiting" in the queue, "Submitted" elsewhere. */
  whenLabel: string;
  rows: ClaimTableRow[];
  /** Make the first row's button stand out: in the queue, the oldest claim is the one to pick up next. */
  highlightFirst?: boolean;
};

// The agent's lists (A02, A11, A12) as a real table, so screen readers can move by row and column and
// read each cell with its heading. On a narrow window the table scrolls sideways inside its card.
export function ClaimTable({ caption, whenLabel, rows, highlightFirst = false }: ClaimTableProps) {
  return (
    <div className={styles.card}>
      <table className={styles.table}>
        <caption className="visually-hidden">{caption}</caption>
        <thead>
          <tr>
            <th scope="col">Reference</th>
            <th scope="col">Customer</th>
            <th scope="col">Claim type</th>
            <th scope="col" className={styles.number}>
              Amount
            </th>
            <th scope="col">Status</th>
            <th scope="col">{whenLabel}</th>
            <th scope="col">
              <span className="visually-hidden">Action</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id}>
              <th scope="row" className={styles.reference}>
                {row.reference}
              </th>
              <td>
                <span className={styles.customer}>
                  <Avatar name={row.customerName} />
                  {row.customerName}
                </span>
              </td>
              <td>{row.claimType}</td>
              <td className={styles.number}>
                <span className={styles.amount}>{formatMoney(row.amount)}</span>
                {row.claimedAmount !== undefined && row.claimedAmount !== row.amount && (
                  <span className={styles.claimed}>of {formatMoney(row.claimedAmount)}</span>
                )}
              </td>
              <td>
                <StatusBadge status={row.status} />
              </td>
              <td className={styles.when}>{row.when}</td>
              <td className={styles.action}>
                <Link to={row.href} className={styles.link} data-first={(highlightFirst && index === 0) || undefined}>
                  {row.actionLabel}
                  <span className="visually-hidden"> {row.reference}</span>
                  <ArrowRight size={16} aria-hidden="true" />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
