import { Bell } from 'lucide-react';
import { Link } from 'react-router';
import { Avatar } from '../Avatar/Avatar';
import styles from './AgentHeader.module.css';

export type AgentNavItem = {
  label: string;
  href: string;
  current?: boolean;
};

type AgentHeaderProps = {
  nav: AgentNavItem[];
  unreadCount: number;
  userName: string;
  /** Shown under the name, for example "Claims officer". */
  userRole: string;
  onSignOut: () => void;
};

// The bar at the top of every agent screen (A02 in Hi-Fi 2.0).
export function AgentHeader({ nav, unreadCount, userName, userRole, onSignOut }: AgentHeaderProps) {
  const bellLabel = unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications';

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <Link className={styles.brand} to="/agent">
          Teduh
        </Link>
        <nav aria-label="Main">
          <ul className={styles.nav}>
            {nav.map((item) => (
              <li key={item.href}>
                <Link
                  className={styles.link}
                  to={item.href}
                  // Tells screen readers which page this is; the CSS styles it too.
                  aria-current={item.current ? 'page' : undefined}
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </div>
      <div className={styles.right}>
        <Link className={styles.bell} to="/agent/notifications" aria-label={bellLabel}>
          <Bell size={18} aria-hidden="true" />
          {unreadCount > 0 && (
            <span className={styles.count} aria-hidden="true">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </Link>
        <span className={styles.divider} aria-hidden="true" />
        <div className={styles.user}>
          <Avatar name={userName} />
          <div className={styles.who}>
            <span className={styles.name}>{userName}</span>
            <span className={styles.role}>{userRole}</span>
          </div>
        </div>
        <span className={styles.divider} aria-hidden="true" />
        <button type="button" className={styles.signOut} onClick={onSignOut}>
          Sign out
        </button>
      </div>
    </header>
  );
}
