import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router';
import {
  demoAccounts,
  DEMO_PASSWORD,
  fetchMe,
  landingAfterSignIn,
  meQueryKey,
  signIn,
  useMe,
  type DemoRole,
} from '../api/auth';
import { Footer } from '../components/Footer/Footer';
import { PageLoading } from '../components/PageStatus/PageStatus';
import { SignInForm, type SignInPending } from '../components/SignInForm/SignInForm';
import { queryClient } from '../queryClient';
import styles from './SignInPage.module.css';

type Credentials = { email: string; password: string; via: SignInPending };

// /sign-in. The demo buttons and the form both make a real sign-in to Spring Security. Afterwards the app
// asks who signed in and goes to that person's start page, or back to the page that sent them here.
export function SignInPage() {
  const me = useMe();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;
  const [via, setVia] = useState<SignInPending>();

  const signInMutation = useMutation({
    mutationFn: ({ email, password }: Credentials) => signIn(email, password),
    onMutate: ({ via }) => setVia(via),
    onSuccess: async () => {
      // Whatever was cached belonged to whoever used this browser before.
      queryClient.clear();
      const person = await queryClient.fetchQuery({ queryKey: meQueryKey, queryFn: fetchMe });
      if (person) {
        void navigate(landingAfterSignIn(person.role, from), { replace: true });
      }
    },
    onSettled: () => setVia(undefined),
  });

  if (me.isPending) return <PageLoading />;
  // Already signed in (for example after pressing Back): go where they belong.
  if (me.data && !signInMutation.isPending) {
    return <Navigate to={landingAfterSignIn(me.data.role, from)} replace />;
  }

  function demo(role: DemoRole) {
    signInMutation.mutate({ email: demoAccounts[role].email, password: DEMO_PASSWORD, via: role });
  }

  return (
    <div className={styles.page}>
      <title>Sign in · Teduh Claims</title>
      <main className={styles.main}>
        <SignInForm
          onDemo={demo}
          onSubmit={(email, password) => signInMutation.mutate({ email, password, via: 'form' })}
          pending={via}
          error={signInMutation.error?.message}
        />
      </main>
      <Footer />
    </div>
  );
}
