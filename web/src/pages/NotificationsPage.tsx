import { Bell, CheckCheck } from 'lucide-react';
import { useInbox, useMarkAllRead, useMarkRead } from '../api/notifications';
import { Button } from '../components/Button/Button';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { NotificationList } from '../components/NotificationList/NotificationList';
import { PageError, SectionLoading } from '../components/PageStatus/PageStatus';
import styles from './Page.module.css';

type NotificationsPageProps = {
  /** Where a notification about this claim leads: the customer's or the agent's claim page. */
  claimPath: (claimId: string) => string;
};

// C11 for customers and A10 for agents. The list is the bell's own data (one cache entry), so marking
// something read here clears the bell straight away. Left out from C11: the Claims and Policies filter
// (every notification is about a claim) and the payout summary (the app makes no payments).
export function NotificationsPage({ claimPath }: NotificationsPageProps) {
  const inbox = useInbox();
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();
  const unread = inbox.data?.unreadCount ?? 0;

  return (
    <div className={styles.page}>
      <title>Notifications · Teduh Claims</title>
      <div className={styles.row}>
        <div className={styles.intro}>
          <h1>Notifications</h1>
          {inbox.data && <p>{unread === 0 ? 'Nothing unread' : `${unread} unread`}</p>}
        </div>
        {unread > 0 && (
          <Button
            variant="secondary"
            size="sm"
            icon={<CheckCheck size={16} aria-hidden="true" />}
            loading={markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            Mark all as read
          </Button>
        )}
      </div>

      {inbox.isPending && <SectionLoading label="Loading your notifications…" />}
      {inbox.isError && <PageError message={inbox.error.message} onRetry={() => void inbox.refetch()} />}
      {inbox.data?.notifications.length === 0 && (
        <EmptyState icon={<Bell size={28} />} title="No notifications yet">
          You'll see an update here whenever a claim moves on.
        </EmptyState>
      )}
      {inbox.data && inbox.data.notifications.length > 0 && (
        <NotificationList
          notifications={inbox.data.notifications.map((item) => ({ ...item, href: claimPath(item.claimId) }))}
          onOpen={(id) => markRead.mutate(id)}
        />
      )}
    </div>
  );
}
