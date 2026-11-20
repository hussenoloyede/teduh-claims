import { Plus, ShieldCheck } from 'lucide-react';
import { coverState, limitsOf, usePolicies, type CoverState } from '../api/policies';
import { ButtonLink } from '../components/Button/Button';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { PageError, SectionLoading } from '../components/PageStatus/PageStatus';
import { PolicyCard } from '../components/PolicyCard/PolicyCard';
import { formatDate, formatMoney, todayInMalaysia } from '../lib/format';
import styles from './Page.module.css';

const stateText: Record<CoverState, string> = {
  active: 'Covers today',
  upcoming: "Hasn't started yet",
  ended: 'Has ended',
};

// C14: each policy with its limit per claim type and its cover dates, all from /api/policies. A customer
// can cover another trip from here, as long as its dates don't overlap a policy they already have.
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
      {policies.data?.length === 0 && (
        <EmptyState
          icon={<ShieldCheck size={28} />}
          title="You don't have a policy yet"
          action={
            <ButtonLink to="/plans" fullWidth>
              Choose a plan
            </ButtonLink>
          }
        >
          Choose a plan for your trip. Once you are covered, you can claim if something goes wrong.
        </EmptyState>
      )}

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

      {policies.data && policies.data.length > 0 && (
        <ButtonLink to="/plans" variant="secondary" fullWidth icon={<Plus size={18} aria-hidden="true" />}>
          Cover another trip
        </ButtonLink>
      )}
    </div>
  );
}
