import { Bell, ChevronRight } from 'lucide-react';
import { Link } from 'react-router';
import { formatDateTime } from '../../lib/format';
import styles from './NotificationList.module.css';

export type NotificationEntry = {
  id: string;
  message: string;
  /** When it was sent, as the API sends it (UTC). */
  createdAt: string;
  read: boolean;
  /** The claim page this notification is about. */
  href: string;
};

type NotificationListProps = {
  notifications: NotificationEntry[];
  /** Called when an unread notification is opened, so it can be marked read. */
  onOpen: (id: string) => void;
};

// The list on C11 and A10. Each notification is a link to its claim. Unread ones carry a visible "New",
// which is also what a screen reader hears, so the tint is never the only sign.
export function NotificationList({ notifications, onOpen }: NotificationListProps) {
  return (
    <ul className={styles.list}>
      {notifications.map((item) => (
        <li key={item.id}>
          <Link
            className={styles.item}
            data-unread={!item.read || undefined}
            to={item.href}
            onClick={() => {
              if (!item.read) onOpen(item.id);
            }}
          >
            <span className={styles.icon} aria-hidden="true">
              <Bell size={18} />
            </span>
            <span className={styles.body}>
              {!item.read && <span className={styles.new}>New</span>}
              <span className={styles.message}>{item.message}</span>
              <span className={styles.time}>{formatDateTime(item.createdAt)}</span>
            </span>
            <ChevronRight className={styles.chevron} size={18} aria-hidden="true" />
          </Link>
        </li>
      ))}
    </ul>
  );
}
