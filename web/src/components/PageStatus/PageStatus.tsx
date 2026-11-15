import { Button } from '../Button/Button';
import styles from './PageStatus.module.css';

// Full-page messages for the moments before a page has its data.

/**
 * Shown while the app finds out who is signed in. The demo server scales to zero when nobody is using it,
 * so the very first request can take a minute or two; the text says so rather than leaving a blank page.
 */
export function PageLoading() {
  return (
    <div className={styles.status} role="status">
      <span className={styles.spinner} aria-hidden="true" />
      <p className={styles.title}>Loading Teduh…</p>
      <p>The demo server sleeps when nobody is using it, so the first visit can take a minute or two.</p>
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
