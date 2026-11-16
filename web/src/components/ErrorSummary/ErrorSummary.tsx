import { CircleAlert } from 'lucide-react';
import type { Ref } from 'react';
import styles from './ErrorSummary.module.css';

export type SummaryError = {
  /** The id of the field the message is about, so the link can move focus there. */
  fieldId: string;
  message: string;
};

type ErrorSummaryProps = {
  errors: SummaryError[];
  /** Lets the form move focus here. In React 19 a ref is an ordinary prop. */
  ref?: Ref<HTMLDivElement>;
};

// The box at the top of a form step when something needs fixing (C05). The form moves focus here after a
// failed Continue, so a screen reader hears what's wrong straight away. Each message is a link that moves
// focus to its field.
export function ErrorSummary({ errors, ref }: ErrorSummaryProps) {
  if (errors.length === 0) return null;
  const title = errors.length === 1 ? 'There is 1 thing to fix' : `There are ${errors.length} things to fix`;
  return (
    <div ref={ref} className={styles.summary} tabIndex={-1} aria-labelledby="error-summary-title">
      <p className={styles.title} id="error-summary-title">
        <CircleAlert size={18} aria-hidden="true" />
        {title}
      </p>
      <ul className={styles.list}>
        {errors.map((error) => (
          <li key={error.fieldId}>
            <a
              href={`#${error.fieldId}`}
              onClick={(event) => {
                // A plain #link scrolls but doesn't move focus into an input, so do it here.
                event.preventDefault();
                document.getElementById(error.fieldId)?.focus();
              }}
            >
              {error.message}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
