import styles from './FilterChips.module.css';

export type FilterOption<T extends string> = { value: T; label: string; count: number };

type FilterChipsProps<T extends string> = {
  /** Read out before the chips, for example "Show claims". */
  label: string;
  options: FilterOption<T>[];
  value: T;
  onChange: (value: T) => void;
};

// The "All claims / In progress / Decided" chips on C13. Each chip is a toggle button; aria-pressed tells a
// screen reader which one is on, and the CSS styles that same attribute.
export function FilterChips<T extends string>({ label, options, value, onChange }: FilterChipsProps<T>) {
  return (
    <div className={styles.chips} role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={styles.chip}
          aria-pressed={option.value === value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          <span className={styles.count}>{option.count}</span>
        </button>
      ))}
    </div>
  );
}
