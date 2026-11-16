import { coverState, limitsOf, usePolicies, type CoverState } from '../api/policies';
import { PageError, SectionLoading } from '../components/PageStatus/PageStatus';
import { PolicyCard } from '../components/PolicyCard/PolicyCard';
import { formatDate, formatMoney, todayInMalaysia } from '../lib/format';
import styles from './Page.module.css';

const stateText: Record<CoverState, string> = {
  active: 'Covers today',
  upcoming: "Hasn't started yet",
  ended: 'Has ended',
};

// C14: each policy with its limit per claim type and its cover dates, all from /api/policies.
export function PoliciesPage() {
  const policies = usePolicies();
  const today = todayInMalaysia();

  return (
    <div className={styles.page}>
      <title>Policies · Teduh Claims</title>
      <div className={styles.intro}>
        <h1>Your policies</h1>
        <p>Your Teduh travel cover and what you can claim.</p>
      </div>

      {policies.isPending && <SectionLoading label="Loading your policies…" />}
      {policies.isError && <PageError message={policies.error.message} onRetry={() => void policies.refetch()} />}
      {policies.data?.length === 0 && <p className={styles.quiet}>You have no Teduh policies.</p>}

      {policies.data?.map((policy) => {
        const state = coverState(policy, today);
        const limits = limitsOf(policy);
        return (
          <section key={policy.policyId} className={styles.section} aria-label={policy.planName}>
            <PolicyCard
              planName={policy.planName}
              policyNumber={policy.policyNumber}
              coverStart={policy.coverStart}
              coverEnd={policy.coverEnd}
              state={state}
              covers={limits.map((limit) => limit.label)}
            />
            <div className={styles.panel}>
              <div className={styles.panelTitle}>
                <h2>Claim limits</h2>
                <p>The most you can claim each time, under {policy.planName}</p>
              </div>
              <dl className={styles.facts}>
                {limits.map((limit) => (
                  <div key={limit.type}>
                    <dt>{limit.label}</dt>
                    <dd>{formatMoney(limit.limit)}</dd>
                  </div>
                ))}
              </dl>
            </div>
            <div className={styles.panel}>
              <div className={styles.panelTitle}>
                <h2>Cover dates</h2>
                <p>
                  Policy {policy.policyNumber}. {stateText[state]}.
                </p>
              </div>
              <dl className={styles.facts}>
                <div>
                  <dt>Starts</dt>
                  <dd>{formatDate(policy.coverStart)}</dd>
                </div>
                <div>
                  <dt>Ends</dt>
                  <dd>{formatDate(policy.coverEnd)}</dd>
                </div>
              </dl>
            </div>
          </section>
        );
      })}
    </div>
  );
}
