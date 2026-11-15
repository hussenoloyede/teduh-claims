import { ArrowRight, Eye, EyeOff, Headset, Info, LockKeyhole, Mail, Umbrella, UserRound } from 'lucide-react';
import { useState, type FormEvent, type ReactNode } from 'react';
import { demoAccounts, DEMO_PASSWORD, type DemoRole } from '../../api/auth';
import { Button } from '../Button/Button';
import { TextField } from '../TextField/TextField';
import styles from './SignInForm.module.css';

/** Which button is waiting for the server: one of the demo buttons, or the form's Sign in. */
export type SignInPending = DemoRole | 'form';

type SignInFormProps = {
  onDemo: (role: DemoRole) => void;
  onSubmit: (email: string, password: string) => void;
  pending?: SignInPending;
  /** The server's answer when signing in failed. */
  error?: string;
};

// C01 and A01 in Hi-Fi 2.0, as one screen for both roles. The design's Remember me, Forgot password,
// security badges and shortcut cards are left out: the app has none of those behind them.
export function SignInForm({ onDemo, onSubmit, pending, error }: SignInFormProps) {
  const [showPassword, setShowPassword] = useState(false);
  const busy = pending !== undefined;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    onSubmit(String(form.get('email') ?? ''), String(form.get('password') ?? ''));
  }

  return (
    <div className={styles.page}>
      <header className={styles.brand}>
        <span className={styles.logo} aria-hidden="true">
          <Umbrella size={30} />
        </span>
        <h1 className={styles.name}>Teduh</h1>
        <p className={styles.tagline}>Travel protected, claim with ease</p>
      </header>

      <section className={styles.card} aria-labelledby="demo-heading">
        <h2 id="demo-heading" className={styles.overline}>
          Try the demo
        </h2>
        <div className={styles.demoButtons}>
          <DemoButton
            role="customer"
            label="Try as customer"
            icon={<UserRound size={16} />}
            pending={pending}
            onDemo={onDemo}
          />
          <DemoButton role="agent" label="Try as agent" icon={<Headset size={16} />} pending={pending} onDemo={onDemo} />
        </div>
        <p className={styles.note}>Signs in to a demo account. Every claim and policy in it is made up.</p>
      </section>

      <p className={styles.divider}>
        <span>Or sign in with email</span>
      </p>

      <form className={styles.card} onSubmit={submit} aria-label="Sign in with email">
        <TextField
          label="Email address"
          name="email"
          type="email"
          autoComplete="username"
          required
          icon={<Mail size={18} />}
        />
        <TextField
          label="Password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          required
          icon={<LockKeyhole size={18} />}
          trailing={
            <button
              type="button"
              className={styles.reveal}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((shown) => !shown)}
            >
              {showPassword ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
              <span className="visually-hidden">Show password</span>
            </button>
          }
        />
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        <Button type="submit" fullWidth loading={pending === 'form'} disabled={busy}>
          Sign in
          <ArrowRight size={16} aria-hidden="true" />
        </Button>
        <p className={styles.tip}>
          <Info size={16} aria-hidden="true" />
          <span>
            Demo accounts use the password <code>{DEMO_PASSWORD}</code>.
          </span>
        </p>
      </form>
    </div>
  );
}

type DemoButtonProps = {
  role: DemoRole;
  label: string;
  icon: ReactNode;
  pending?: SignInPending;
  onDemo: (role: DemoRole) => void;
};

function DemoButton({ role, label, icon, pending, onDemo }: DemoButtonProps) {
  const waiting = pending === role;
  return (
    <button
      type="button"
      className={styles.demo}
      onClick={() => onDemo(role)}
      disabled={pending !== undefined}
      aria-busy={waiting || undefined}
    >
      <span className={styles.demoLabel}>
        <span aria-hidden="true">{icon}</span>
        {waiting ? 'Signing in…' : label}
      </span>
      <span className={styles.demoWho}>{demoAccounts[role].name}</span>
    </button>
  );
}
