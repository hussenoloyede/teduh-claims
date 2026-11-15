import { Navigate, Outlet, useLocation } from 'react-router';
import { homeFor, useMe } from '../api/auth';
import { useInbox } from '../api/notifications';
import { BottomNav, type CustomerSection } from '../components/BottomNav/BottomNav';
import { CustomerHeader } from '../components/CustomerHeader/CustomerHeader';
import { Footer } from '../components/Footer/Footer';
import { PageError, PageLoading } from '../components/PageStatus/PageStatus';
import styles from './Layout.module.css';
import { sectionOf } from './navigation';

// Every customer page sits inside this: the header with the bell, the page, the footer and the bottom tabs.
// It is also the guard: someone signed out goes to sign in (and comes back here after), an agent goes to
// the agent pages.
export function CustomerLayout() {
  const me = useMe();
  const location = useLocation();

  if (me.isPending) return <PageLoading />;
  if (me.isError) return <PageError message={me.error.message} onRetry={() => void me.refetch()} />;
  if (!me.data) return <Navigate to="/sign-in" replace state={{ from: location.pathname }} />;
  if (me.data.role !== 'CUSTOMER') return <Navigate to={homeFor(me.data.role)} replace />;

  return <CustomerShell userName={me.data.displayName} section={sectionOf(location.pathname)} />;
}

function CustomerShell({ userName, section }: { userName: string; section?: CustomerSection }) {
  const inbox = useInbox();
  return (
    <div className={styles.shell}>
      <CustomerHeader unreadCount={inbox.data?.unreadCount ?? 0} userName={userName} />
      <main className={styles.main}>
        <Outlet />
      </main>
      <Footer />
      <div className={styles.bottomNav}>
        <BottomNav current={section} />
      </div>
    </div>
  );
}
