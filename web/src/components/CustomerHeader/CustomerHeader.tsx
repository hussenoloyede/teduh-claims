import { Bell, ChevronLeft } from 'lucide-react';
import { Avatar } from '../Avatar/Avatar';
import styles from './CustomerHeader.module.css';

type CustomerHeaderProps = {
  /** Shows a Back link to this address instead of the Teduh name. */
  backHref?: string;
  unreadCount: number;
  /** The customer's full name, for the profile initials. Leave out to hide them. */
  userName?: string;
};

// The bar at the top of every customer screen (C02, C07, C13 in Hi-Fi 2.0).
export function CustomerHeader({ backHref, unreadCount, userName }: CustomerHeaderProps) {
  const bellLabel = unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications';

  return (
    <header className={styles.header}>
      {backHref ? (
        <a className={styles.back} href={backHref}>
          <ChevronLeft size={22} aria-hidden="true" />
          Back
        </a>
      ) : (
        <a className={styles.brand} href="/">
          Teduh
        </a>
      )}
      <div className={styles.actions}>
        <a className={styles.iconLink} href="/notifications" aria-label={bellLabel}>
          <Bell size={20} aria-hidden="true" />
          {unreadCount > 0 && <span className={styles.dot} />}
        </a>
        {userName && (
          // A hidden word instead of aria-label: the initials are a stand-in for a
          // photo, not a label, and axe asks that visible text never fights the name.
          <a className={styles.iconLink} href="/profile">
            <span className="visually-hidden">Profile</span>
            <Avatar name={userName} />
          </a>
        )}
      </div>
    </header>
  );
}
