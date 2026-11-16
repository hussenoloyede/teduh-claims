import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, FileQuestion, Send } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import { useMe } from '../../api/auth';
import {
  claimQueryKey,
  claimTypeLabel,
  myClaimsQueryKey,
  useClaim,
  type ClaimDetails,
} from '../../api/claims';
import { api, unwrap } from '../../api/client';
import { inboxQueryKey } from '../../api/notifications';
import { ApiError } from '../../api/problem';
import { Banner } from '../../components/Banner/Banner';
import { Button, ButtonLink } from '../../components/Button/Button';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { FileDrop } from '../../components/FileDrop/FileDrop';
import { PageError, SectionLoading } from '../../components/PageStatus/PageStatus';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import { TextArea } from '../../components/TextArea/TextArea';
import { Timeline } from '../../components/Timeline/Timeline';
import { formatDate, formatMoney } from '../../lib/format';
import { queryClient } from '../../queryClient';
import { checkFile, type PickedFile } from '../newClaim/newClaimForm';
import page from '../Page.module.css';
import { ClaimDocuments } from './ClaimDocuments';
import { latestRequest, timelineItems } from './claimTimeline';
import styles from './ClaimPage.module.css';
import { checkResponse, filesAllowed, MAX_NOTE, toResponseFormData } from './respondForm';

// /claims/:claimId, the customer's view of one claim: C07 (just submitted), C08 (more information needed,
// with the respond form), C09 (approved), C10 (rejected) and C12 (not found).
//
// Left out from the designs: copying the reference, the payout and DuitNow text, "Download summary (PDF)",
// dispute and re-review, "Speak with an advocate", SLA times and the regulator lines. The app does none
// of these.
export function ClaimPage() {
  const { claimId = '' } = useParams();
  const claim = useClaim(claimId);
  const me = useMe();
  const justSubmitted = Boolean((useLocation().state as { submitted?: boolean } | null)?.submitted);

  if (claim.isPending) return <SectionLoading label="Loading your claim…" />;
  if (claim.isError) {
    if (claim.error instanceof ApiError && claim.error.status === 404) return <ClaimNotFound />;
    return <PageError message={claim.error.message} onRetry={() => void claim.refetch()} />;
  }

  return <ClaimView claim={claim.data} viewerId={me.data?.id ?? ''} justSubmitted={justSubmitted} />;
}

type ClaimViewProps = { claim: ClaimDetails; viewerId: string; justSubmitted: boolean };

function ClaimView({ claim, viewerId, justSubmitted }: ClaimViewProps) {
  const agent = claim.assignedAgentName ?? 'your agent';
  // Set when the customer has just answered a request, so the page says it went through.
  const [sentTo, setSentTo] = useState<string>();

  return (
    <div className={page.page}>
      <title>{`${claim.reference} · Teduh Claims`}</title>
      <Link className={styles.back} to="/claims">
        <ArrowLeft size={16} aria-hidden="true" />
        Your claims
      </Link>

      {justSubmitted && (
        <Banner variant="success" title="Claim submitted">
          Your reference is {claim.reference}. You'll get a notification when an agent starts reviewing it.
        </Banner>
      )}

      <section className={page.panel} aria-labelledby="claim-title">
        <div className={styles.head}>
          <div>
            <h1 id="claim-title" className={styles.reference}>
              {claim.reference}
            </h1>
            <p className={styles.plan}>
              {claim.policy.planName} ·{' '}
              <span className={styles.nowrap}>Policy {claim.policy.policyNumber}</span>
            </p>
          </div>
          <StatusBadge status={claim.status} />
        </div>
        <dl className={page.facts}>
          <div>
            <dt>What happened</dt>
            <dd>{claimTypeLabel[claim.claimType]}</dd>
          </div>
          <div>
            <dt>Claimed amount</dt>
            <dd>{formatMoney(claim.amountClaimed)}</dd>
          </div>
          <div>
            <dt>Incident date</dt>
            <dd>{formatDate(claim.incidentDate)}</dd>
          </div>
          <div>
            <dt>Submitted</dt>
            <dd>{formatDate(claim.submittedAt)}</dd>
          </div>
        </dl>
        <blockquote className={styles.quote}>{claim.description}</blockquote>
      </section>

      {sentTo && (
        <Banner variant="success" title={`Sent to ${sentTo}`}>
          Your claim is back under review. You'll get a notification when there's news.
        </Banner>
      )}

      <StatusSection claim={claim} agent={agent} onSent={() => setSentTo(agent)} />

      <section className={page.section} aria-labelledby="claim-documents">
        <h2 id="claim-documents" className={styles.sectionTitle}>
          Documents <span className={styles.count}>{claim.documents.length}</span>
        </h2>
        <ClaimDocuments claim={claim} />
      </section>

      <section className={page.panel} aria-labelledby="claim-history">
        <h2 id="claim-history" className={styles.sectionTitle}>
          History
        </h2>
        <Timeline items={timelineItems(claim, viewerId)} />
      </section>
    </div>
  );
}

/** What the claim's status means for the customer, and what they can do. */
type StatusSectionProps = { claim: ClaimDetails; agent: string; onSent: () => void };

function StatusSection({ claim, agent, onSent }: StatusSectionProps) {
  switch (claim.status) {
    case 'SUBMITTED':
      return (
        <Banner title="What happens next">
          An agent will pick up your claim and start the review. You'll get a notification when they do.
        </Banner>
      );
    case 'UNDER_REVIEW':
      return (
        <Banner title="What happens next">
          {agent} is reviewing your claim. You'll get a notification when they decide, or if they need anything
          else from you.
        </Banner>
      );
    case 'MORE_INFO_NEEDED':
      return <MoreInfoNeeded claim={claim} agent={agent} onSent={onSent} />;
    case 'APPROVED':
      return (
        <section className={styles.decision} data-outcome="approved" aria-labelledby="decision-title">
          <h2 id="decision-title">Approved</h2>
          <p className={styles.amount}>
            {formatMoney(claim.approvedAmount ?? claim.amountClaimed)}
            {claim.approvedAmount != null && claim.approvedAmount < claim.amountClaimed && (
              <span className={styles.claimedWas}>of {formatMoney(claim.amountClaimed)} claimed</span>
            )}
          </p>
          {claim.decisionReason && (
            <figure className={styles.reason}>
              <blockquote>{claim.decisionReason}</blockquote>
              <figcaption>{agent}</figcaption>
            </figure>
          )}
          <p className={styles.note}>
            This is the final decision on your claim. Teduh is a demo, so no money is paid.
          </p>
        </section>
      );
    case 'REJECTED':
      return (
        <section className={styles.decision} data-outcome="rejected" aria-labelledby="decision-title">
          <h2 id="decision-title">Rejected</h2>
          {claim.decisionReason && (
            <figure className={styles.reason}>
              <blockquote>{claim.decisionReason}</blockquote>
              <figcaption>{agent}</figcaption>
            </figure>
          )}
          <p className={styles.note}>This is the final decision on your claim.</p>
        </section>
      );
  }
}

/** C08: what the agent asked for, and the form to answer with a note, files or both. */
function MoreInfoNeeded({ claim, agent, onSent }: StatusSectionProps) {
  const request = latestRequest(claim);
  const [note, setNote] = useState('');
  const [files, setFiles] = useState<PickedFile[]>([]);
  const [problem, setProblem] = useState<string>();
  const allowed = filesAllowed(claim.documents.length);

  const send = useMutation({
    mutationFn: () => {
      const response = { version: claim.version, note: note.trim() || null };
      return unwrap(
        api.POST('/api/claims/{id}/respond', {
          params: { path: { id: claim.id } },
          body: { response },
          bodySerializer: () => toResponseFormData(response, files),
        }),
      );
    },
    onSuccess: (updated) => {
      onSent();
      queryClient.setQueryData(claimQueryKey(claim.id), updated);
      void queryClient.invalidateQueries({ queryKey: myClaimsQueryKey });
      void queryClient.invalidateQueries({ queryKey: inboxQueryKey });
    },
  });

  function submit() {
    const found = checkResponse(note, files, allowed);
    setProblem(found);
    if (!found) send.mutate();
  }

  return (
    <>
      <section className={styles.request} aria-labelledby="request-title">
        <h2 id="request-title">{agent} needs more information</h2>
        {request?.message && <blockquote>{request.message}</blockquote>}
      </section>

      <form
        className={page.panel}
        aria-labelledby="respond-title"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        <div className={page.panelTitle}>
          <h2 id="respond-title">Send what was asked for</h2>
          <p>Add a note, files, or both. PDF, JPG or PNG, up to 10 MB each.</p>
        </div>
        {problem && (
          <Banner variant="error" title="Not sent yet">
            {problem}
          </Banner>
        )}
        {send.isError && (
          <Banner variant="error" title="Not sent">
            {send.error.message}
          </Banner>
        )}
        <TextArea
          label="Your note (optional)"
          hint={`Up to ${MAX_NOTE} characters.`}
          value={note}
          onChange={(event) => setNote(event.target.value)}
          count={{ used: note.length, max: MAX_NOTE }}
        />
        <FileDrop
          max={allowed}
          files={files.map((picked) => ({ id: picked.id, name: picked.file.name, size: picked.file.size, problem: picked.problem }))}
          onAdd={(added) =>
            setFiles((current) => [
              ...current,
              ...added.map((file) => ({ id: crypto.randomUUID(), file, problem: checkFile(file) })),
            ])
          }
          onRemove={(id) => setFiles((current) => current.filter((picked) => picked.id !== id))}
        />
        <Button type="submit" fullWidth loading={send.isPending} icon={<Send size={16} aria-hidden="true" />}>
          {`Send to ${agent}`}
        </Button>
      </form>
    </>
  );
}

/**
 * C12. The API answers 404 both for a claim that doesn't exist and for someone else's, so this page can't
 * tell them apart either, and says so.
 */
function ClaimNotFound() {
  return (
    <div className={page.page}>
      <title>Claim not found · Teduh Claims</title>
      <EmptyState
        icon={<FileQuestion size={28} />}
        title="Claim not found"
        action={<ButtonLink to="/claims">Back to your claims</ButtonLink>}
      >
        We couldn't find that claim. For privacy, this page looks the same whether a claim doesn't exist or
        belongs to someone else.
      </EmptyState>
    </div>
  );
}
