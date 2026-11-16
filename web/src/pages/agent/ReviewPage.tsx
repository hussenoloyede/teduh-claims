import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, Check, FileQuestion, MessageSquareText, Play, RefreshCw, X } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { useMe } from '../../api/auth';
import {
  claimQueryKey,
  claimTypeLabel,
  isDecided,
  queuesQueryKey,
  useClaim,
  type ClaimDetails,
} from '../../api/claims';
import { api, unwrap } from '../../api/client';
import { inboxQueryKey } from '../../api/notifications';
import { ApiError } from '../../api/problem';
import { Banner } from '../../components/Banner/Banner';
import { Button, ButtonLink } from '../../components/Button/Button';
import { Dialog } from '../../components/Dialog/Dialog';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { PageError, SectionLoading } from '../../components/PageStatus/PageStatus';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import { TextArea } from '../../components/TextArea/TextArea';
import { TextField } from '../../components/TextField/TextField';
import { Timeline } from '../../components/Timeline/Timeline';
import { formatDate, formatDateTime, formatMoney, formatWaiting } from '../../lib/format';
import { queryClient } from '../../queryClient';
import { ClaimDocuments } from '../claim/ClaimDocuments';
import { latestRequest, timelineItems } from '../claim/claimTimeline';
import page from '../Page.module.css';
import styles from './AgentPages.module.css';
import {
  amountText,
  checkApproval,
  checkRejection,
  checkRequest,
  isPartial,
  MAX_MESSAGE,
  parseAmount,
  type ApprovalProblems,
} from './reviewForms';

// /agent/claims/:claimId, the agent's view of one claim: A04 (submitted, start the review), A05 (in review,
// with the actions), A06 to A08 (the three dialogs) and A09 (someone else changed the claim: reload).
//
// Left out from the designs: SLA countdowns and queue priority, exclusive locks and auto-release, ID
// numbers, "verified" and "forensics" labels, OCR'd receipt lines and totals, flight tracking, fraud scores,
// quick-insert chips, WhatsApp and SMS, DuitNow and bank payouts, version inspectors and diffs. The app has
// none of these.

type Action =
  | { kind: 'start' }
  | { kind: 'request'; message: string }
  | { kind: 'approve'; amount: number; reason: string | null }
  | { kind: 'reject'; reason: string };

type DialogKind = 'request' | 'approve' | 'reject';

/** Every action sends back the version the agent was looking at. A newer one on the server means 409. */
function perform(claim: ClaimDetails, action: Action): Promise<ClaimDetails> {
  const params = { path: { id: claim.id } };
  const version = claim.version;
  switch (action.kind) {
    case 'start':
      return unwrap(api.POST('/api/claims/{id}/start-review', { params, body: { version } }));
    case 'request':
      return unwrap(api.POST('/api/claims/{id}/request-info', { params, body: { version, message: action.message } }));
    case 'approve':
      return unwrap(
        api.POST('/api/claims/{id}/approve', { params, body: { version, amount: action.amount, reason: action.reason } }),
      );
    case 'reject':
      return unwrap(api.POST('/api/claims/{id}/reject', { params, body: { version, reason: action.reason } }));
  }
}

/** What the page says once an action has gone through. */
function doneMessage(action: Action, customer: string): { title: string; text: string } {
  switch (action.kind) {
    case 'start':
      return { title: 'Review started', text: `The claim is assigned to you. ${customer} will get a notification.` };
    case 'request':
      return {
        title: 'Request sent',
        text: `${customer} will get a notification. The claim waits on them until they reply.`,
      };
    case 'approve':
      return { title: 'Claim approved', text: `${customer} will get a notification with the amount.` };
    case 'reject':
      return { title: 'Claim rejected', text: `${customer} will get a notification with your reason.` };
  }
}

function isConflict(error: unknown): boolean {
  return error instanceof ApiError && error.status === 409;
}

export function ReviewPage() {
  const { claimId = '' } = useParams();
  const claim = useClaim(claimId);
  const me = useMe();

  if (claim.isPending) return <SectionLoading label="Loading the claim…" />;
  if (claim.isError) {
    if (claim.error instanceof ApiError && claim.error.status === 404) return <ReviewNotFound />;
    return <PageError message={claim.error.message} onRetry={() => void claim.refetch()} />;
  }

  return (
    <Review
      claim={claim.data}
      meId={me.data?.id ?? ''}
      reloading={claim.isFetching}
      onReload={() => claim.refetch()}
    />
  );
}

type ReviewProps = {
  claim: ClaimDetails;
  meId: string;
  reloading: boolean;
  onReload: () => Promise<unknown>;
};

function Review({ claim, meId, reloading, onReload }: ReviewProps) {
  const [dialog, setDialog] = useState<DialogKind>();
  const [done, setDone] = useState<{ title: string; text: string }>();
  const [conflict, setConflict] = useState(false);

  const act = useMutation({
    mutationFn: (action: Action) => perform(claim, action),
    onMutate: () => setDone(undefined),
    onSuccess: (updated, action) => {
      queryClient.setQueryData(claimQueryKey(claim.id), updated);
      void queryClient.invalidateQueries({ queryKey: queuesQueryKey });
      void queryClient.invalidateQueries({ queryKey: inboxQueryKey });
      setDialog(undefined);
      setDone(doneMessage(action, claim.customerName));
    },
    onError: (error) => {
      // A 409 means the claim moved on (another agent, or the customer answered). Nothing the agent typed
      // can be sent as it is, so the dialog closes and the page offers a reload.
      if (isConflict(error)) {
        setDialog(undefined);
        setConflict(true);
      }
    },
  });

  // 422 and other refusals are shown where the agent acted: in the dialog, or under Start review.
  const actionError = act.isError && !isConflict(act.error) ? act.error.message : undefined;

  function open(kind: DialogKind) {
    act.reset();
    setDialog(kind);
  }

  async function reload() {
    await onReload();
    setConflict(false);
    act.reset();
  }

  const mine = claim.assignedAgentId === meId;
  const back = backLink(claim, mine);
  const decidedEntry = isDecided(claim.status) ? claim.timeline.at(-1) : undefined;

  return (
    <div className={`${page.page} ${page.wide}`}>
      <title>{`${claim.reference} · Teduh Claims`}</title>
      <Link className={styles.back} to={back.href}>
        <ArrowLeft size={16} aria-hidden="true" />
        {back.label}
      </Link>

      {conflict && (
        <Banner
          variant="error"
          title="This claim changed while you were looking at it"
          action={
            <Button size="sm" loading={reloading} icon={<RefreshCw size={16} aria-hidden="true" />} onClick={() => void reload()}>
              Reload claim
            </Button>
          }
        >
          Reload to see the latest before you act. Nothing was changed by you.
        </Banner>
      )}
      {done && !conflict && (
        <Banner variant="success" title={done.title}>
          {done.text}
        </Banner>
      )}

      <section className={`${page.panel} ${styles.claimHead}`} aria-labelledby="claim-title">
        <div className={styles.titleRow}>
          <h1 id="claim-title" className={styles.reference}>
            {claim.reference}
          </h1>
          <StatusBadge status={claim.status} />
          <span className={styles.assignment} data-mine={mine || undefined}>
            {mine ? 'Assigned to you' : claim.assignedAgentName ? `Assigned to ${claim.assignedAgentName}` : 'Not assigned'}
          </span>
        </div>
        <p className={styles.meta}>
          Submitted {formatDateTime(claim.submittedAt)}
          {decidedEntry
            ? ` · decided ${formatDate(decidedEntry.occurredAt)}`
            : ` · waiting ${formatWaiting(claim.submittedAt)}`}
        </p>
      </section>

      <div className={styles.columns}>
        <div className={styles.main}>
          <ClaimSummaryPanel claim={claim} />

          <section className={page.panel} aria-labelledby="statement-title">
            <h2 id="statement-title" className={styles.panelTitle}>
              {claim.customerName}'s statement
            </h2>
            <blockquote className={styles.statement}>{claim.description}</blockquote>
          </section>

          <section className={page.panel} aria-labelledby="documents-title">
            <h2 id="documents-title" className={styles.panelTitle}>
              Documents <span className={styles.count}>{claim.documents.length}</span>
            </h2>
            <ClaimDocuments claim={claim} />
          </section>
        </div>

        <aside className={styles.side}>
          <section className={page.panel} aria-labelledby="actions-title">
            <h2 id="actions-title" className={styles.panelTitle}>
              {isDecided(claim.status) ? 'Decision' : 'Review'}
            </h2>
            <ActionPanel
              claim={claim}
              mine={mine}
              blocked={conflict}
              starting={act.isPending && act.variables?.kind === 'start'}
              startError={dialog ? undefined : actionError}
              onStart={() => act.mutate({ kind: 'start' })}
              onOpen={open}
            />
          </section>

          <section className={page.panel} aria-labelledby="history-title">
            <h2 id="history-title" className={styles.panelTitle}>
              History
            </h2>
            <Timeline items={timelineItems(claim, meId)} />
          </section>
        </aside>
      </div>

      {dialog === 'request' && (
        <RequestDialog
          claim={claim}
          busy={act.isPending}
          error={actionError}
          onClose={() => setDialog(undefined)}
          onSend={(message) => act.mutate({ kind: 'request', message })}
        />
      )}
      {dialog === 'approve' && (
        <ApproveDialog
          claim={claim}
          busy={act.isPending}
          error={actionError}
          onClose={() => setDialog(undefined)}
          onApprove={(amount, reason) => act.mutate({ kind: 'approve', amount, reason })}
        />
      )}
      {dialog === 'reject' && (
        <RejectDialog
          claim={claim}
          busy={act.isPending}
          error={actionError}
          onClose={() => setDialog(undefined)}
          onReject={(reason) => act.mutate({ kind: 'reject', reason })}
        />
      )}
    </div>
  );
}

/** Back to the list the claim is on for this agent. */
function backLink(claim: ClaimDetails, mine: boolean): { href: string; label: string } {
  if (mine && isDecided(claim.status)) return { href: '/agent/history', label: 'History' };
  if (mine) return { href: '/agent/assigned', label: 'Assigned reviews' };
  return { href: '/agent/queue', label: 'Claims queue' };
}

/** Who, which policy, what happened, and how the amount sits against the limit for this type of claim. */
function ClaimSummaryPanel({ claim }: { claim: ClaimDetails }) {
  const { policy } = claim;
  const type = claimTypeLabel[claim.claimType];
  const share = policy.limit ? Math.min(100, (claim.amountClaimed / policy.limit) * 100) : undefined;

  return (
    <section className={page.panel} aria-labelledby="summary-title">
      <h2 id="summary-title" className={styles.panelTitle}>
        Claim and policy
      </h2>
      <dl className={page.facts}>
        <div>
          <dt>Customer</dt>
          <dd>{claim.customerName}</dd>
        </div>
        <div>
          <dt>Plan</dt>
          <dd>{policy.planName}</dd>
          <dd className={styles.small}>Policy {policy.policyNumber}</dd>
        </div>
        <div>
          <dt>Claim type</dt>
          <dd>{type}</dd>
        </div>
        <div>
          <dt>Incident date</dt>
          <dd>{formatDate(claim.incidentDate)}</dd>
        </div>
      </dl>
      <div className={styles.limit}>
        <p>
          <strong className={styles.claimedAmount}>{formatMoney(claim.amountClaimed)}</strong> claimed
          {policy.limit != null && (
            <>
              {' '}
              of the {formatMoney(policy.limit)} limit for {type.toLowerCase()}
            </>
          )}
        </p>
        {share !== undefined && (
          <div className={styles.bar} aria-hidden="true">
            <span style={{ width: `${share}%` }} />
          </div>
        )}
        <p className={styles.small}>
          Covered {formatDate(policy.coverStart)} to {formatDate(policy.coverEnd)}
        </p>
      </div>
    </section>
  );
}

type ActionPanelProps = {
  claim: ClaimDetails;
  mine: boolean;
  blocked: boolean;
  starting: boolean;
  startError?: string;
  onStart: () => void;
  onOpen: (kind: DialogKind) => void;
};

/** What this agent can do with the claim right now, which depends on its status and who it's assigned to. */
function ActionPanel({ claim, mine, blocked, starting, startError, onStart, onOpen }: ActionPanelProps) {
  const agent = claim.assignedAgentName ?? 'Another agent';

  if (blocked) {
    return <p className={styles.small}>Actions are paused until you reload the claim.</p>;
  }

  switch (claim.status) {
    case 'SUBMITTED':
      return (
        <div className={styles.actions}>
          <p className={styles.small}>
            Starting the review assigns this claim to you. Other agents can still open it, but only you can act on
            it until you decide.
          </p>
          {startError && <Banner variant="error">{startError}</Banner>}
          <Button fullWidth loading={starting} icon={<Play size={16} aria-hidden="true" />} onClick={onStart}>
            Start review
          </Button>
        </div>
      );
    case 'UNDER_REVIEW':
      if (!mine) return <p className={styles.small}>{agent} is reviewing this claim. Only they can act on it.</p>;
      return (
        <div className={styles.actions}>
          <p className={styles.small}>Approve and Reject ask you to confirm first. A decision is final.</p>
          <Button variant="secondary" fullWidth icon={<MessageSquareText size={16} aria-hidden="true" />} onClick={() => onOpen('request')}>
            Request more information
          </Button>
          <Button fullWidth icon={<Check size={16} aria-hidden="true" />} onClick={() => onOpen('approve')}>
            Approve
          </Button>
          <Button variant="danger" fullWidth icon={<X size={16} aria-hidden="true" />} onClick={() => onOpen('reject')}>
            Reject
          </Button>
        </div>
      );
    case 'MORE_INFO_NEEDED': {
      if (!mine) return <p className={styles.small}>{agent} is waiting on {claim.customerName} for more information.</p>;
      const request = latestRequest(claim);
      return (
        <div className={styles.actions}>
          <p>Waiting on {claim.customerName}. You asked:</p>
          {request?.message && <blockquote className={styles.request}>{request.message}</blockquote>}
          <p className={styles.small}>
            When they reply, the claim comes back to In review and you get a notification.
          </p>
        </div>
      );
    }
    case 'APPROVED':
    case 'REJECTED': {
      const approved = claim.status === 'APPROVED';
      const amount = claim.approvedAmount ?? claim.amountClaimed;
      return (
        <div className={styles.decision} data-outcome={approved ? 'approved' : 'rejected'}>
          <p className={styles.outcome}>
            {approved ? `Approved ${formatMoney(amount)}` : 'Rejected'}
            {approved && amount < claim.amountClaimed && (
              <span className={styles.small}> of {formatMoney(claim.amountClaimed)} claimed</span>
            )}
          </p>
          {claim.decisionReason && (
            <figure className={styles.reason}>
              <blockquote>{claim.decisionReason}</blockquote>
              <figcaption>{mine ? 'Your reason, shown to the customer' : agent}</figcaption>
            </figure>
          )}
          <p className={styles.small}>Decisions are final. The claim can't be reopened.</p>
        </div>
      );
    }
  }
}

type DialogBaseProps = { claim: ClaimDetails; busy: boolean; error?: string; onClose: () => void };

function subtitleOf(claim: ClaimDetails): string {
  return `${claim.reference} · ${claimTypeLabel[claim.claimType]} · ${formatMoney(claim.amountClaimed)}`;
}

/** A06: ask the customer for something. They see the message on their claim page. */
function RequestDialog({ claim, busy, error, onClose, onSend }: DialogBaseProps & { onSend: (message: string) => void }) {
  const [message, setMessage] = useState('');
  const [problem, setProblem] = useState<string>();

  function send() {
    const found = checkRequest(message);
    setProblem(found);
    if (!found) onSend(message.trim());
  }

  return (
    <Dialog
      open
      title={`Ask ${claim.customerName} for more information`}
      subtitle={subtitleOf(claim)}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={busy} onClick={send}>
            Send request
          </Button>
        </>
      }
    >
      <p className={styles.small}>
        {claim.customerName} sees this on their claim page and gets a notification. The claim waits on them until they
        reply with a note, files or both.
      </p>
      {error && <Banner variant="error">{error}</Banner>}
      <TextArea
        label="What do you need?"
        hint="Be specific, for example which document and what it should show."
        rows={4}
        value={message}
        error={problem}
        onChange={(event) => {
          setMessage(event.target.value);
          setProblem(undefined);
        }}
        count={{ used: message.length, max: MAX_MESSAGE }}
      />
    </Dialog>
  );
}

type ApproveDialogProps = DialogBaseProps & { onApprove: (amount: number, reason: string | null) => void };

/** A07: approve the full amount, or less with a reason the customer sees. */
function ApproveDialog({ claim, busy, error, onClose, onApprove }: ApproveDialogProps) {
  const [amount, setAmount] = useState(amountText(claim.amountClaimed));
  const [reason, setReason] = useState('');
  const [problems, setProblems] = useState<ApprovalProblems>({});
  const partial = isPartial(amount, claim.amountClaimed);
  const typed = parseAmount(amount);

  function approve() {
    const found = checkApproval(amount, reason, claim.amountClaimed);
    setProblems(found);
    if (!found.amount && !found.reason && typed !== undefined) onApprove(typed, reason.trim() || null);
  }

  return (
    <Dialog
      open
      title={`Approve claim ${claim.reference}`}
      subtitle={`${claim.customerName} · ${claimTypeLabel[claim.claimType]}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={busy} icon={<Check size={16} aria-hidden="true" />} onClick={approve}>
            {typed !== undefined && typed > 0 && typed <= claim.amountClaimed ? `Approve ${formatMoney(typed)}` : 'Approve'}
          </Button>
        </>
      }
    >
      <dl className={page.facts}>
        <div>
          <dt>Amount claimed</dt>
          <dd>{formatMoney(claim.amountClaimed)}</dd>
        </div>
        {claim.policy.limit != null && (
          <div>
            <dt>Policy limit for {claimTypeLabel[claim.claimType].toLowerCase()}</dt>
            <dd>{formatMoney(claim.policy.limit)}</dd>
          </div>
        )}
      </dl>
      {error && <Banner variant="error">{error}</Banner>}
      <TextField
        label="Amount to approve"
        hint={`Up to ${formatMoney(claim.amountClaimed)}, the amount claimed.`}
        inputMode="decimal"
        icon={<span className={styles.currency}>RM</span>}
        value={amount}
        error={problems.amount}
        onChange={(event) => {
          setAmount(event.target.value);
          setProblems({});
        }}
      />
      {partial && (
        <Banner title="Less than claimed">
          {`This is less than the ${formatMoney(claim.amountClaimed)} claimed, so ${claim.customerName} needs a reason.`}
        </Banner>
      )}
      <TextArea
        label={partial ? 'Reason (shown to the customer)' : 'Reason (optional, shown to the customer)'}
        rows={3}
        value={reason}
        error={problems.reason}
        onChange={(event) => {
          setReason(event.target.value);
          setProblems((current) => ({ ...current, reason: undefined }));
        }}
        count={{ used: reason.length, max: MAX_MESSAGE }}
      />
      <p className={styles.small}>This is the final decision. Teduh is a demo, so no money is paid.</p>
    </Dialog>
  );
}

/** A08: reject, always with a reason the customer sees. */
function RejectDialog({ claim, busy, error, onClose, onReject }: DialogBaseProps & { onReject: (reason: string) => void }) {
  const [reason, setReason] = useState('');
  const [problem, setProblem] = useState<string>();

  function reject() {
    const found = checkRejection(reason);
    setProblem(found);
    if (!found) onReject(reason.trim());
  }

  return (
    <Dialog
      open
      tone="danger"
      title={`Reject claim ${claim.reference}`}
      subtitle={subtitleOf(claim)}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="danger" loading={busy} icon={<X size={16} aria-hidden="true" />} onClick={reject}>
            Reject claim
          </Button>
        </>
      }
    >
      <Banner variant="warning" title="This decision is final">
        {`The claim can't be reopened. ${claim.customerName} will see your reason on their claim page.`}
      </Banner>
      {error && <Banner variant="error">{error}</Banner>}
      <TextArea
        label="Reason (shown to the customer)"
        hint="Say which part of the policy or which evidence the decision rests on."
        rows={4}
        value={reason}
        error={problem}
        onChange={(event) => {
          setReason(event.target.value);
          setProblem(undefined);
        }}
        count={{ used: reason.length, max: MAX_MESSAGE }}
      />
    </Dialog>
  );
}

/** Agents can open any claim, so a 404 here really means there is no such claim. */
function ReviewNotFound() {
  return (
    <div className={page.page}>
      <title>Claim not found · Teduh Claims</title>
      <EmptyState
        icon={<FileQuestion size={28} />}
        title="Claim not found"
        action={<ButtonLink to="/agent/queue">Back to the claims queue</ButtonLink>}
      >
        There is no claim at this address. Check the link, or pick the claim from the queue.
      </EmptyState>
    </div>
  );
}
