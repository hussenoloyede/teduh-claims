import { ChevronRight, CircleHelp, LogOut } from 'lucide-react';
import { Link } from 'react-router';
import { useMe } from '../api/auth';
import { useMyClaims } from '../api/claims';
import { coverState, usePolicies } from '../api/policies';
import { Avatar } from '../components/Avatar/Avatar';
import { Button } from '../components/Button/Button';
import { todayInMalaysia } from '../lib/format';
import { useSignOut } from '../useSignOut';
import page from './Page.module.css';
import styles from './ProfilePage.module.css';

// C15: who is signed in, how many policies and claims they have, and the way out. Customers sign out here;
// agents use the header.
export function ProfilePage() {
  const me = useMe();
  const policies = usePolicies();
  const claims = useMyClaims();
  const signOut = useSignOut();
  if (!me.data) return null; // CustomerLayout has already made sure someone is signed in.

  const today = todayInMalaysia();
  const active = policies.data?.filter((policy) => coverState(policy, today) === 'active').length;

  return (
    <div className={page.page}>
      <title>Profile · Teduh Claims</title>
      <div className={page.intro}>
        <h1>Profile</h1>
        <p>Your account in this demo.</p>
      </div>
      <section className={page.panel} aria-label="Your account">
        <div className={styles.who}>
          <Avatar name={me.data.displayName} />
          <div className={styles.name}>
            <p className={styles.displayName}>{me.data.displayName}</p>
            <p className={styles.email}>{me.data.email}</p>
          </div>
          <span className={styles.role}>Customer</span>
        </div>
        <dl className={page.facts}>
          <div>
            <dt>Policies</dt>
            <dd>{active === undefined ? '…' : `${active} active`}</dd>
          </div>
          <div>
            <dt>Claims</dt>
            <dd>{claims.data ? `${claims.data.length} made` : '…'}</dd>
          </div>
        </dl>
      </section>
      <Link to="/help" className={page.linkCard}>
        <CircleHelp size={20} aria-hidden="true" />
        <span>
          <strong>Help and questions</strong>
          <span>What a plan covers and how claims work</span>
        </span>
        <ChevronRight size={20} aria-hidden="true" />
      </Link>
      <Button
        fullWidth
        icon={<LogOut size={16} aria-hidden="true" />}
        loading={signOut.isPending}
        onClick={() => signOut.mutate()}
      >
        Sign out
      </Button>
    </div>
  );
}
