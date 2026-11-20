import styles from './Stepper.module.css';

type StepperProps = {
  /** 1-based. */
  current: number;
  steps: string[];
  /** What the progress bar is for, read by screen readers. */
  label?: string;
};

// "Step 2 of 4 · What happened" and a bar, as on C03 to C06. The bar is a progressbar for screen readers,
// with the same words as its value, so it never reads out as a bare percentage.
export function Stepper({ current, steps, label: barLabel = 'Claim form progress' }: StepperProps) {
  const label = `Step ${current} of ${steps.length}`;
  return (
    <div className={styles.stepper}>
      <div className={styles.labels}>
        <span className={styles.count}>{label}</span>
        <span className={styles.name}>{steps[current - 1]}</span>
      </div>
      <div
        className={styles.track}
        role="progressbar"
        aria-label={barLabel}
        aria-valuemin={1}
        aria-valuemax={steps.length}
        aria-valuenow={current}
        aria-valuetext={`${label}: ${steps[current - 1]}`}
      >
        <div className={styles.fill} style={{ width: `${(current / steps.length) * 100}%` }} />
      </div>
    </div>
  );
}
