import { Check } from 'lucide-react';
import { useId, type ComponentPropsWithoutRef } from 'react';
import { formatPlanMoney } from '../../lib/format';
import styles from './PlanCard.module.css';

type PlanCardProps = Omit<ComponentPropsWithoutRef<'input'>, 'type'> & {
  title: string;
  blurb: string;
  /** The demo price in ringgit. Nothing is charged. */
  price: number;
  /** The most Teduh pays per claim type, in the order to show them. */
  limits: { label: string; limit: number }[];
};

// One plan on C16. Like ChoiceCard it is a real radio button, so arrow keys move between plans. The radio's
// name is just the plan name; the blurb, price and limits are its description, read after the name. The
// label stretches over the whole card, so a click anywhere on it picks the plan.
export function PlanCard({ title, blurb, price, limits, className, ...input }: PlanCardProps) {
  const id = useId();
  return (
    <div className={[styles.card, className].filter(Boolean).join(' ')}>
      <label className={styles.top}>
        <input
          {...input}
          type="radio"
          className={styles.input}
          aria-describedby={`${id}-blurb ${id}-price ${id}-limits`}
        />
        <span className={styles.name}>{title}</span>
        <span className={styles.tick} aria-hidden="true">
          <Check size={14} strokeWidth={3} />
        </span>
      </label>
      <p className={styles.blurb} id={`${id}-blurb`}>
        {blurb}
      </p>
      <p className={styles.price} id={`${id}-price`}>
        <span className={styles.amount}>{formatPlanMoney(price)}</span> per trip, demo price
      </p>
      <dl className={styles.limits} id={`${id}-limits`}>
        {limits.map((row) => (
          <div key={row.label}>
            <dt>{row.label}</dt>
            <dd>{formatPlanMoney(row.limit)}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
