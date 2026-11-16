import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react';
import type { ReactNode } from 'react';
import styles from './Banner.module.css';

type BannerProps = {
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  children?: ReactNode;
  /** A button or link at the end, for example Reload on the 409 banner. */
  action?: ReactNode;
};

const icons = { info: Info, success: CircleCheck, warning: TriangleAlert, error: CircleAlert };

// A message across the top of a page or form. An error is an alert, read out as soon as it appears; a
// success is a status, read out politely. Each variant has its own icon as well as its colour.
export function Banner({ variant = 'info', title, children, action }: BannerProps) {
  const Icon = icons[variant];
  return (
    <div className={styles.banner} data-variant={variant} role={variant === 'error' ? 'alert' : 'status'}>
      <Icon className={styles.icon} size={20} aria-hidden="true" />
      <div className={styles.body}>
        {title && <p className={styles.title}>{title}</p>}
        {children && <div className={styles.text}>{children}</div>}
        {action && <div className={styles.action}>{action}</div>}
      </div>
    </div>
  );
}
