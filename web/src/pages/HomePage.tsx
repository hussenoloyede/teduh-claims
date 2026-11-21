import { ChevronRight, CircleHelp, FileText, Lightbulb, Plus, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router';
import { greetingName, useMe } from '../api/auth';
import { isDecided, useMyClaims } from '../api/claims';
import { coverState, limitsOf, planNames, usePolicies } from '../api/policies';
import { ButtonLink } from '../components/Button/Button';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { PageError, SectionLoading } from '../components/PageStatus/PageStatus';
import { PolicyCard } from '../components/PolicyCard/PolicyCard';
import { SectionHeader } from '../components/SectionHeader/SectionHeader';
import { todayInMalaysia } from '../lib/format';
import { ClaimCardList } from './claimCards';
import styles from './Page.module.css';

// C02, and C20 for a new customer who has no policy yet: then the page offers the plans instead of a
// claim, since a claim needs a policy. Left out although the design shows them: the emergency hotline (Teduh has none), and the promise of
// payment "in 2 to 3 working days via DuitNow" (the app makes no payments).
export function HomePage() {
  const me = useMe();
  const policies = usePolicies();
  const claims = useMyClaims();
  const today = todayInMalaysia();

  const firstName = me.data ? greetingName(me.data.displayName) : undefined;
  const activeCount = policies.data?.filter((policy) => coverState(policy, today) === 'active').length ?? 0;
  const openCount = claims.data?.filter((claim) => !isDecided(claim.status)).length ?? 0;
  const noPolicy = policies.data?.length === 0;

  return (
    <div className={styles.page}>
      <title>Home · Teduh Claims</title>
      <div className={styles.intro}>
        <h1>Hi, {firstName}</h1>
        <p>Your travel cover and your claims, in one place.</p>
      </div>

      <section className={styles.section} aria-labelledby="home-policies">
        <SectionHeader
          id="home-policies"
          title="Your policies"
          badge={policies.data && !noPolicy ? `${activeCount} active` : undefined}
          link={noPolicy ? undefined : { to: '/policies', label: 'View all' }}
        />
        {noPolicy && (
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
        {policies.isPending && <SectionLoading label="Loading your policies…" />}
        {policies.isError && <PageError message={policies.error.message} onRetry={() => void policies.refetch()} />}
        {policies.data?.map((policy) => (
          <PolicyCard
            key={policy.policyId}
            planName={policy.planName}
            policyNumber={policy.policyNumber}
            coverStart={policy.coverStart}
            coverEnd={policy.coverEnd}
            state={coverState(policy, today)}
            covers={limitsOf(policy).map((limit) => limit.label)}
            href="/policies"
          />
        ))}
      </section>

      <section className={styles.section} aria-labelledby="home-claims">
        <SectionHeader
          id="home-claims"
          title="Your claims"
          badge={claims.data && !noPolicy ? `${openCount} in progress` : undefined}
          link={claims.data?.length ? { to: '/claims', label: 'All claims' } : undefined}
        />
        {claims.isPending && <SectionLoading label="Loading your claims…" />}
        {claims.isError && <PageError message={claims.error.message} onRetry={() => void claims.refetch()} />}
        {claims.data?.length === 0 && noPolicy && (
          <EmptyState icon={<FileText size={28} />} title="No claims yet">
            After you have a policy, your claims show here.
          </EmptyState>
        )}
        {claims.data?.length === 0 && !noPolicy && (
          <EmptyState icon={<FileText size={28} />} title="You haven't made a claim yet">
            If something went wrong on your trip, start a claim and attach your receipts.
          </EmptyState>
        )}
        {claims.data && claims.data.length > 0 && (
          <ClaimCardList claims={claims.data.slice(0, 3)} plans={planNames(policies.data)} />
        )}
      </section>

      {noPolicy ? (
        <Link to="/help" className={styles.linkCard}>
          <CircleHelp size={20} aria-hidden="true" />
          <span>
            <strong>New to Teduh?</strong>
            <span>What a plan covers and how claims work</span>
          </span>
          <ChevronRight size={20} aria-hidden="true" />
        </Link>
      ) : (
        <ButtonLink to="/claims/new" fullWidth icon={<Plus size={18} aria-hidden="true" />}>
          Start a claim
        </ButtonLink>
      )}

      <aside className={styles.tip} aria-label="Tip">
        <Lightbulb size={20} aria-hidden="true" />
        <div>
          <strong>Before you claim</strong>
          <p>
            Keep your receipts and any report from the airline. A clear, flat photo or a PDF of each one makes the
            review easier.
          </p>
        </div>
      </aside>
    </div>
  );
}
