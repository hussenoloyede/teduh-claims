import { ClipboardList, History, Inbox } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { claimTypeLabel, useQueue, type ClaimSummary } from '../../api/claims';
import { ClaimTable, type ClaimTableRow } from '../../components/ClaimTable/ClaimTable';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { FilterChips } from '../../components/FilterChips/FilterChips';
import { PageError, SectionLoading } from '../../components/PageStatus/PageStatus';
import { formatDate, formatWaiting } from '../../lib/format';
import page from '../Page.module.css';
import styles from './AgentPages.module.css';

// The agent's three lists, one per header link. Each is a tab (or two) of the API's queue:
//   Claims queue (A02)      submitted               claims nobody has picked up, oldest first
//   Assigned reviews (A11)  in-review-by-me         claims this agent is reviewing
//                           waiting-on-customer     ...and those waiting for the customer's answer
//   History (A12)           decided                 claims this agent approved or rejected
//
// Left out from the designs: the KPI cards (SLA breach risk, triage pace), "Overdue", Export, the
// "Filter current list" box, the policy numbers under each name, and the rule cards (SLA targets, a
// second sign-off above RM 3,000, a 45-minute lock). The app has none of these.

function toRow(claim: ClaimSummary, when: string, actionLabel: string): ClaimTableRow {
  return {
    id: claim.id,
    reference: claim.reference,
    customerName: claim.customerName,
    claimType: claimTypeLabel[claim.claimType],
    amount: claim.approvedAmount ?? claim.amountClaimed,
    claimedAmount: claim.amountClaimed,
    status: claim.status,
    when,
    href: `/agent/claims/${claim.id}`,
    actionLabel,
  };
}

type ListPageProps = { title: string; intro: string; count?: number; children: ReactNode };

function ListPage({ title, intro, count, children }: ListPageProps) {
  return (
    <div className={`${page.page} ${page.wide}`}>
      <title>{`${title} · Teduh Claims`}</title>
      <div className={page.intro}>
        <h1 className={styles.heading}>
          {title}
          {count !== undefined && <span className={styles.count}>{count}</span>}
        </h1>
        <p>{intro}</p>
      </div>
      {children}
    </div>
  );
}

/** A02: every submitted claim, oldest first, so the longest wait is picked up first. */
export function QueuePage() {
  const queue = useQueue('submitted');
  return (
    <ListPage
      title="Claims queue"
      intro="Claims waiting for an agent, oldest first. Starting a review assigns a claim to you."
      count={queue.data?.length}
    >
      {queue.isPending && <SectionLoading label="Loading the queue…" />}
      {queue.isError && <PageError message={queue.error.message} onRetry={() => void queue.refetch()} />}
      {queue.data?.length === 0 && (
        <EmptyState icon={<Inbox size={28} />} title="No claims waiting">
          New claims appear here as customers submit them.
        </EmptyState>
      )}
      {queue.data && queue.data.length > 0 && (
        <ClaimTable
          caption="Claims waiting for review, oldest first"
          whenLabel="Waiting"
          highlightFirst
          rows={queue.data.map((claim) => toRow(claim, formatWaiting(claim.submittedAt), 'Open claim'))}
        />
      )}
      <p className={styles.footnote}>The list updates every 30 seconds.</p>
    </ListPage>
  );
}

type AssignedFilter = 'all' | 'review' | 'waiting';

/** A11: the claims this agent has started reviewing and hasn't decided yet. */
export function AssignedPage() {
  const inReview = useQueue('in-review-by-me');
  const waiting = useQueue('waiting-on-customer');
  const [filter, setFilter] = useState<AssignedFilter>('all');

  const failed = inReview.error ?? waiting.error;
  const review = inReview.data ?? [];
  const onCustomer = waiting.data ?? [];
  const all = [...review, ...onCustomer].sort((a, b) => a.submittedAt.localeCompare(b.submittedAt));
  const shown = filter === 'review' ? review : filter === 'waiting' ? onCustomer : all;
  const loaded = inReview.data && waiting.data;

  return (
    <ListPage
      title="Assigned reviews"
      intro="Claims you have started reviewing, oldest first. Only you can act on them until you decide."
      count={loaded ? all.length : undefined}
    >
      {!loaded && !failed && <SectionLoading label="Loading your reviews…" />}
      {failed && (
        <PageError
          message={failed.message}
          onRetry={() => {
            void inReview.refetch();
            void waiting.refetch();
          }}
        />
      )}
      {loaded && all.length === 0 && (
        <EmptyState icon={<ClipboardList size={28} />} title="Nothing assigned to you">
          Open a claim from the queue and start a review. It will be listed here until you decide.
        </EmptyState>
      )}
      {loaded && all.length > 0 && (
        <>
          <FilterChips
            label="Show reviews"
            value={filter}
            onChange={setFilter}
            options={[
              { value: 'all', label: 'All assigned', count: all.length },
              { value: 'review', label: 'In review', count: review.length },
              { value: 'waiting', label: 'Waiting on customer', count: onCustomer.length },
            ]}
          />
          {shown.length > 0 ? (
            <ClaimTable
              caption="Claims assigned to you"
              whenLabel="Submitted"
              rows={shown.map((claim) =>
                toRow(
                  claim,
                  formatDate(claim.submittedAt),
                  claim.status === 'UNDER_REVIEW' ? 'Continue review' : 'Open claim',
                ),
              )}
            />
          ) : (
            <p className={page.quiet}>
              {filter === 'review' ? 'No claims in review right now.' : 'No claims waiting on a customer.'}
            </p>
          )}
        </>
      )}
    </ListPage>
  );
}

type HistoryFilter = 'all' | 'approved' | 'rejected';

/** A12: the claims this agent has decided, newest first. */
export function HistoryPage() {
  const decided = useQueue('decided');
  const [filter, setFilter] = useState<HistoryFilter>('all');

  // The API sends every tab oldest first; history reads better the other way round.
  const all = [...(decided.data ?? [])].sort((a, b) => b.submittedAt.localeCompare(a.submittedAt));
  const approved = all.filter((claim) => claim.status === 'APPROVED');
  const rejected = all.filter((claim) => claim.status === 'REJECTED');
  const shown = filter === 'approved' ? approved : filter === 'rejected' ? rejected : all;

  return (
    <ListPage
      title="History"
      intro="Claims you have approved or rejected, newest first. Decisions are final."
      count={decided.data?.length}
    >
      {decided.isPending && <SectionLoading label="Loading your decisions…" />}
      {decided.isError && <PageError message={decided.error.message} onRetry={() => void decided.refetch()} />}
      {decided.data?.length === 0 && (
        <EmptyState icon={<History size={28} />} title="No decisions yet">
          Claims you approve or reject are listed here.
        </EmptyState>
      )}
      {all.length > 0 && (
        <>
          <FilterChips
            label="Show decisions"
            value={filter}
            onChange={setFilter}
            options={[
              { value: 'all', label: 'All decided', count: all.length },
              { value: 'approved', label: 'Approved', count: approved.length },
              { value: 'rejected', label: 'Rejected', count: rejected.length },
            ]}
          />
          {shown.length > 0 ? (
            <ClaimTable
              caption="Claims you have decided, newest first"
              whenLabel="Submitted"
              rows={shown.map((claim) => toRow(claim, formatDate(claim.submittedAt), 'View claim'))}
            />
          ) : (
            <p className={page.quiet}>
              {filter === 'approved' ? 'No approved claims yet.' : 'No rejected claims yet.'}
            </p>
          )}
        </>
      )}
    </ListPage>
  );
}
