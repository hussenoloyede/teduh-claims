import { ArrowRight, CalendarDays, Plane, Umbrella } from 'lucide-react';
import { Link } from 'react-router';
import type { CoverState } from '../../api/policies';
import { formatDate } from '../../lib/format';
import styles from './PolicyCard.module.css';

type PolicyCardProps = {
  planName: string;
  policyNumber: string;
  /** "2026-10-01", as the API sends dates. */
  coverStart: string;
  coverEnd: string;
  state: CoverState;
  /** The claim types the policy covers, as labels. */
  covers: string[];
  /** Where the arrow goes. Leave out on the policy's own page. */
  href?: string;
};

const stateLabel: Record<CoverState, string> = { active: 'Active', upcoming: 'Not started', ended: 'Ended' };

// The dark policy card on C02 and C14 in Hi-Fi 2.0. The design's chips listed medical cover and
// "Worldwide", which Teduh's policies don't have; these chips list the claim types the policy really covers.
export function PolicyCard({ planName, policyNumber, coverStart, coverEnd, state, covers, href }: PolicyCardProps) {
  return (
    <article className={styles.card} aria-label={planName}>
      <Umbrella className={styles.watermark} size={120} aria-hidden="true" />
      <div className={styles.top}>
        <span className={styles.icon} aria-hidden="true">
          <Plane size={20} />
        </span>
        <div className={styles.name}>
          <span className={styles.overline}>Travel insurance</span>
          <h3>{planName}</h3>
        </div>
        <span className={styles.state} data-state={state}>
          {stateLabel[state]}
        </span>
      </div>
      <div className={styles.box}>
        <div className={styles.reference}>
          <span>Policy number</span>
          <span className={styles.number}>{policyNumber}</span>
        </div>
        <p className={styles.dates}>
          <CalendarDays size={14} aria-hidden="true" />
          Covered {formatDate(coverStart)} to {formatDate(coverEnd)}
        </p>
      </div>
      <div className={styles.bottom}>
        <ul className={styles.chips} aria-label="Covers">
          {covers.map((label) => (
            <li key={label}>{label}</li>
          ))}
        </ul>
        {href && (
          <Link className={styles.more} to={href}>
            <ArrowRight size={16} aria-hidden="true" />
            <span className="visually-hidden">{`${planName} details`}</span>
          </Link>
        )}
      </div>
    </article>
  );
}
