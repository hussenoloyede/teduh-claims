import { useId, type ComponentPropsWithoutRef, type ReactNode } from 'react';
import styles from './TextField.module.css';

type TextFieldProps = ComponentPropsWithoutRef<'input'> & {
  label: string;
  /** Help shown under the field, for example the format expected. */
  hint?: string;
  /** Shown under the field in red, and announced by screen readers with the field. */
  error?: string;
  /** A small icon inside the field, before the text. Decorative only. */
  icon?: ReactNode;
  /** A control inside the field, after the text, for example a show-password button. */
  trailing?: ReactNode;
};

// A labelled text input. The label, hint and error are tied to the input with ids, so a screen reader
// reads all three when the field gets focus.
export function TextField({ label, hint, error, icon, trailing, id, className, ...input }: TextFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-error`;
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
      </label>
      <div className={[styles.box, error && styles.invalid].filter(Boolean).join(' ')}>
        {icon && (
          <span className={styles.icon} aria-hidden="true">
            {icon}
          </span>
        )}
        <input
          {...input}
          id={inputId}
          className={styles.input}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
        />
        {trailing && <span className={styles.trailing}>{trailing}</span>}
      </div>
      {hint && (
        <p className={styles.hint} id={hintId}>
          {hint}
        </p>
      )}
      {error && (
        <p className={styles.error} id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}
