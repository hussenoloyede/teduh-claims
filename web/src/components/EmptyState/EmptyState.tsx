import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

type EmptyStateProps = {
  icon: ReactNode;
  title: string;
  children?: ReactNode;
  /** A button or link under the text. */
  action?: ReactNode;
};

// What a list shows when there's nothing in it yet: what's missing and, where there is one, what to do.
export function EmptyState({ icon, title, children, action }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <span className={styles.icon} aria-hidden="true">
        {icon}
      </span>
      <p className={styles.title}>{title}</p>
      {children && <div className={styles.text}>{children}</div>}
      {action}
    </div>
  );
}
