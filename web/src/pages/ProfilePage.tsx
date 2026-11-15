import { LogOut } from 'lucide-react';
import { useMe } from '../api/auth';
import { Avatar } from '../components/Avatar/Avatar';
import { Button } from '../components/Button/Button';
import { useSignOut } from '../useSignOut';
import styles from './ProfilePage.module.css';

// /profile for customers: who is signed in, and the way out. Customers sign out here; agents use the
// header. The rest of C15 (contact details, preferences) has nothing behind it yet, so it's left out.
export function ProfilePage() {
  const me = useMe();
  const signOut = useSignOut();
  if (!me.data) return null; // CustomerLayout has already made sure someone is signed in.

  return (
    <div className={styles.page}>
      <title>Profile · Teduh Claims</title>
      <h1>Profile</h1>
      <section className={styles.card} aria-label="Your account">
        <Avatar name={me.data.displayName} />
        <dl className={styles.facts}>
          <div>
            <dt>Name</dt>
            <dd>{me.data.displayName}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{me.data.email}</dd>
          </div>
          <div>
            <dt>Account</dt>
            <dd>Demo customer</dd>
          </div>
        </dl>
      </section>
      <Button
        variant="secondary"
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
