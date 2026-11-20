import { Umbrella } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Button } from '../Button/Button';
import styles from './PageStatus.module.css';

// Full-page messages for the moments before a page has its data.

/** How long the splash waits before saying the server may be asleep. A server that is awake answers sooner. */
export const WAKING_DELAY_MS = 2000;

/**
 * S01, the splash: shown while the app finds out who is signed in. The demo server scales to zero when
 * nobody is using it, so the very first request can take a minute. If the answer hasn't come after two
 * seconds, the splash says so rather than leaving the visitor looking at a spinner.
 */
export function PageLoading() {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setSlow(true), WAKING_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <div className={styles.splash} role="status">
      <div className={styles.brand}>
        <span className={styles.logo} aria-hidden="true">
          <Umbrella size={32} />
        </span>
        <p className={styles.name}>Teduh</p>
        <p>Travel protected, claim with ease</p>
      </div>
      <div className={styles.waiting}>
        <span className={styles.splashSpinner} aria-hidden="true" />
        <p>{slow ? 'Waking the demo server. The first visit can take a minute.' : 'Loading Teduh…'}</p>
        <p className={styles.fictional}>Teduh is a fictional insurer made for a portfolio project.</p>
      </div>
    </div>
  );
}

type PageErrorProps = {
  message: string;
  onRetry: () => void;
};

export function PageError({ message, onRetry }: PageErrorProps) {
  return (
    <div className={styles.status} role="alert">
      <p className={styles.title}>Something went wrong</p>
      <p>{message}</p>
      <Button variant="secondary" onClick={onRetry}>
        Try again
      </Button>
    </div>
  );
}

/** A small loading line inside a page, for one section's data. */
export function SectionLoading({ label }: { label: string }) {
  return (
    <p className={styles.section} role="status">
      <span className={styles.smallSpinner} aria-hidden="true" />
      {label}
    </p>
  );
}
