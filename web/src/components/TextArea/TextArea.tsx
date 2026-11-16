import { useId, type ComponentPropsWithoutRef } from 'react';
import styles from './TextArea.module.css';

type TextAreaProps = ComponentPropsWithoutRef<'textarea'> & {
  label: string;
  hint?: string;
  error?: string;
  /** Shows "154 / 500" under the box. The limit itself is in the hint, which is read with the field. */
  count?: { used: number; max: number };
};

// A labelled multi-line field, built the same way as TextField: the label, hint and error are tied to the
// box with ids. The character count is visible only; screen readers get the limit from the hint instead
// of hearing a number after every key.
export function TextArea({ label, hint, error, count, id, className, ...textarea }: TextAreaProps) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  const hintId = `${fieldId}-hint`;
  const errorId = `${fieldId}-error`;
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      <label className={styles.label} htmlFor={fieldId}>
        {label}
      </label>
      {hint && (
        <p className={styles.hint} id={hintId}>
          {hint}
        </p>
      )}
      <div className={[styles.box, error && styles.invalid].filter(Boolean).join(' ')}>
        <textarea
          {...textarea}
          id={fieldId}
          className={styles.textarea}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
        />
        {count && (
          <span className={styles.count} aria-hidden="true" data-over={count.used > count.max || undefined}>
            {count.used} / {count.max}
          </span>
        )}
      </div>
      {error && (
        <p className={styles.error} id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}
