import { Navigate, Outlet, useLocation } from 'react-router';
import { homeFor, useMe } from '../api/auth';
import { useInbox } from '../api/notifications';
import { AgentHeader } from '../components/AgentHeader/AgentHeader';
import { Footer } from '../components/Footer/Footer';
import { PageError, PageLoading } from '../components/PageStatus/PageStatus';
import { useSignOut } from '../useSignOut';
import styles from './Layout.module.css';
import { agentNav } from './navigation';

// Every agent page sits inside this, in the agent colours (data-theme="agent" switches the tokens).
// Like CustomerLayout, it is also the guard for the agent pages.
export function AgentLayout() {
  const me = useMe();
  const location = useLocation();

  if (me.isPending) return <PageLoading />;
  if (me.isError) return <PageError message={me.error.message} onRetry={() => void me.refetch()} />;
  if (!me.data) return <Navigate to="/sign-in" replace state={{ from: location.pathname }} />;
  if (me.data.role !== 'AGENT') return <Navigate to={homeFor(me.data.role)} replace />;

  return <AgentShell userName={me.data.displayName} pathname={location.pathname} />;
}

function AgentShell({ userName, pathname }: { userName: string; pathname: string }) {
  const inbox = useInbox();
  const signOut = useSignOut();
  return (
    <div className={styles.shell} data-theme="agent">
      <AgentHeader
        nav={agentNav(pathname)}
        unreadCount={inbox.data?.unreadCount ?? 0}
        userName={userName}
        userRole="Claims officer"
        onSignOut={() => signOut.mutate()}
      />
      <main className={styles.main}>
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
