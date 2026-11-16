import { Check } from 'lucide-react';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import styles from './ChoiceCard.module.css';

type ChoiceCardProps = Omit<ComponentPropsWithoutRef<'input'>, 'type'> & {
  title: string;
  /** A short line on the right, for example "Up to RM 1,000.00". */
  aside?: string;
  description?: string;
  icon?: ReactNode;
};

// One option in a group of radio buttons, drawn as a card (the claim types and policies on C03). It is a
// real radio input inside a label, so arrow keys move between cards and a screen reader says "1 of 4".
// Put the cards in a <fieldset> with a <legend> asking the question.
export function ChoiceCard({ title, aside, description, icon, className, ...input }: ChoiceCardProps) {
  return (
    <label className={[styles.card, className].filter(Boolean).join(' ')}>
      <input {...input} type="radio" className={styles.input} />
      {icon && (
        <span className={styles.icon} aria-hidden="true">
          {icon}
        </span>
      )}
      <span className={styles.text}>
        <span className={styles.top}>
          <span className={styles.title}>{title}</span>
          {aside && <span className={styles.aside}>{aside}</span>}
        </span>
        {description && <span className={styles.description}>{description}</span>}
      </span>
      <span className={styles.tick} aria-hidden="true">
        <Check size={14} strokeWidth={3} />
      </span>
    </label>
  );
}
