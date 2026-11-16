import { FileText, Plus } from 'lucide-react';
import { useState } from 'react';
import { useLocation } from 'react-router';
import { isDecided, useMyClaims } from '../api/claims';
import { planNames, usePolicies } from '../api/policies';
import { Banner } from '../components/Banner/Banner';
import { ButtonLink } from '../components/Button/Button';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { FilterChips } from '../components/FilterChips/FilterChips';
import { PageError, SectionLoading } from '../components/PageStatus/PageStatus';
import { ClaimCardList } from './claimCards';
import styles from './Page.module.css';

type Filter = 'all' | 'open' | 'decided';

const emptyMessage: Record<Exclude<Filter, 'all'>, string> = {
  open: 'No claims in progress.',
  decided: 'No decided claims yet.',
};

// C13. The filter runs in the browser: the API already sends all of the customer's claims, newest first.
export function ClaimsPage() {
  const claims = useMyClaims();
  const policies = usePolicies();
  const [filter, setFilter] = useState<Filter>('all');
  // Set by the new-claim form after a successful submit.
  const submitted = (useLocation().state as { submitted?: string } | null)?.submitted;

  const all = claims.data ?? [];
  const open = all.filter((claim) => !isDecided(claim.status));
  const decided = all.filter((claim) => isDecided(claim.status));
  const shown = filter === 'open' ? open : filter === 'decided' ? decided : all;

  return (
    <div className={styles.page}>
      <title>Claims · Teduh Claims</title>
      <div className={styles.intro}>
        <h1>Your claims</h1>
        <p>Every claim you have made, newest first.</p>
      </div>

      {submitted && (
        <Banner variant="success" title={`Claim ${submitted} submitted`}>
          You'll get a notification when an agent starts reviewing it.
        </Banner>
      )}

      {claims.isPending && <SectionLoading label="Loading your claims…" />}
      {claims.isError && <PageError message={claims.error.message} onRetry={() => void claims.refetch()} />}

      {claims.data?.length === 0 && (
        <EmptyState
          icon={<FileText size={28} />}
          title="You haven't made a claim yet"
          action={
            <ButtonLink to="/claims/new" icon={<Plus size={18} aria-hidden="true" />}>
              Start a claim
            </ButtonLink>
          }
        >
          If something went wrong on your trip, start a claim and attach your receipts.
        </EmptyState>
      )}

      {all.length > 0 && (
        <>
          <FilterChips
            label="Show claims"
            value={filter}
            onChange={setFilter}
            options={[
              { value: 'all', label: 'All claims', count: all.length },
              { value: 'open', label: 'In progress', count: open.length },
              { value: 'decided', label: 'Decided', count: decided.length },
            ]}
          />
          {shown.length > 0 ? (
            <ClaimCardList claims={shown} plans={planNames(policies.data)} />
          ) : (
            <p className={styles.quiet}>{filter !== 'all' && emptyMessage[filter]}</p>
          )}
          <ButtonLink to="/claims/new" fullWidth icon={<Plus size={18} aria-hidden="true" />}>
            Start a claim
          </ButtonLink>
        </>
      )}
    </div>
  );
}
