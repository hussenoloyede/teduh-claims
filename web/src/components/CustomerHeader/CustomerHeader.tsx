import { Bell, ChevronLeft } from 'lucide-react';
import { Link } from 'react-router';
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
        <Link className={styles.back} to={backHref}>
          <ChevronLeft size={22} aria-hidden="true" />
          Back
        </Link>
      ) : (
        <Link className={styles.brand} to="/">
          Teduh
        </Link>
      )}
      <div className={styles.actions}>
        <Link className={styles.iconLink} to="/notifications" aria-label={bellLabel}>
          <Bell size={20} aria-hidden="true" />
          {unreadCount > 0 && <span className={styles.dot} />}
        </Link>
        {userName && (
          // A hidden word instead of aria-label: the initials are a stand-in for a
          // photo, not a label, and axe asks that visible text never fights the name.
          <Link className={styles.iconLink} to="/profile">
            <span className="visually-hidden">Profile</span>
            <Avatar name={userName} />
          </Link>
        )}
      </div>
    </header>
  );
}
