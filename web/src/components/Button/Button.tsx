import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import { Link, type LinkProps } from 'react-router';
import styles from './Button.module.css';

type ButtonProps = ComponentPropsWithoutRef<'button'> & {
  variant?: 'primary' | 'secondary' | 'danger';
  /** md is 48 px tall (customer screens), sm is 32 px (agent tables). */
  size?: 'md' | 'sm';
  fullWidth?: boolean;
  /** Shows a spinner and blocks clicks while a request is in flight. */
  loading?: boolean;
  /** An icon before the label, for example <Download size={16} />. */
  icon?: ReactNode;
};

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  icon,
  type = 'button',
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classes = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth && styles.fullWidth,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button
      {...rest}
      // A plain button by default, so it never submits a form by accident.
      type={type}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {loading ? <span className={styles.spinner} aria-hidden="true" /> : icon}
      {children}
    </button>
  );
}

type ButtonLinkProps = LinkProps & {
  variant?: 'primary' | 'secondary';
  fullWidth?: boolean;
  icon?: ReactNode;
};

/**
 * A link that looks like a button, for actions that go to another page ("Start a claim"). It stays a link,
 * so screen readers announce it as one, and it opens in a new tab like any link.
 */
export function ButtonLink({ variant = 'primary', fullWidth = false, icon, className, children, ...rest }: ButtonLinkProps) {
  const classes = [styles.button, styles[variant], styles.md, fullWidth && styles.fullWidth, className]
    .filter(Boolean)
    .join(' ');
  return (
    <Link {...rest} className={classes}>
      {icon}
      {children}
    </Link>
  );
}
