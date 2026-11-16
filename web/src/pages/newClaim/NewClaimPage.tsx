import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, CalendarX, Clock, Luggage, PackageX, Pencil, Plane, Send } from 'lucide-react';
import { useEffect, useRef, useState, type ReactNode, type RefObject } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { claimTypeLabel, myClaimsQueryKey, type ClaimType } from '../../api/claims';
import { api, unwrap } from '../../api/client';
import { inboxQueryKey } from '../../api/notifications';
import { coverState, limitsOf, usePolicies, type Policy } from '../../api/policies';
import { Banner } from '../../components/Banner/Banner';
import { Button } from '../../components/Button/Button';
import { ChoiceCard } from '../../components/ChoiceCard/ChoiceCard';
import { ErrorSummary, type SummaryError } from '../../components/ErrorSummary/ErrorSummary';
import { FileDrop } from '../../components/FileDrop/FileDrop';
import { PageError, SectionLoading } from '../../components/PageStatus/PageStatus';
import { Stepper } from '../../components/Stepper/Stepper';
import { TextArea } from '../../components/TextArea/TextArea';
import { TextField } from '../../components/TextField/TextField';
import { formatDate, formatFileSize, formatMoney, todayInMalaysia } from '../../lib/format';
import { queryClient } from '../../queryClient';
import page from '../Page.module.css';
import {
  checkAmount,
  checkDescription,
  checkFile,
  checkFiles,
  checkIncidentDate,
  latestIncidentDate,
  MAX_DESCRIPTION,
  MAX_FILES,
  stepFields,
  stepNames,
  toFormData,
  toSubmitRequest,
  type NewClaimValues,
} from './newClaimForm';
import styles from './NewClaimPage.module.css';

const typeIcon: Record<ClaimType, ReactNode> = {
  FLIGHT_DELAY: <Clock size={20} />,
  BAGGAGE_DELAYED: <Luggage size={20} />,
  LOST_OR_DAMAGED_BAGGAGE: <PackageX size={20} />,
  TRIP_CANCELLATION: <CalendarX size={20} />,
};

const typeDescription: Record<ClaimType, string> = {
  FLIGHT_DELAY: 'Your flight left much later than planned.',
  BAGGAGE_DELAYED: 'Your checked bag arrived late and you had to buy things you needed.',
  LOST_OR_DAMAGED_BAGGAGE: 'Your bag was lost, or arrived damaged.',
  TRIP_CANCELLATION: 'You had to cancel the trip before you left.',
};

/** The id each field's error links to from the error summary. */
const fieldId: Record<keyof NewClaimValues, string> = {
  policyId: 'field-policy',
  claimType: 'field-claim-type',
  incidentDate: 'field-incident-date',
  amount: 'field-amount',
  description: 'field-description',
  files: 'field-files',
  confirmed: 'field-confirmed',
};

// /claims/new: C03 to C06 as one form in four steps. All four steps share one React Hook Form instance, so
// going back keeps what was typed. Each step checks its own fields before moving on; Submit checks them all
// again, and the server checks everything once more.
//
// Left out from the design: DuitNow and payout timing, "reviewed within 24 hours", the Bank Negara and
// NRIC lines. The app makes no payments and promises no times.
export function NewClaimPage() {
  const policies = usePolicies();
  const navigate = useNavigate();
  const today = todayInMalaysia();
  const [step, setStep] = useState(0);
  const [showSummary, setShowSummary] = useState(false);
  const summaryRef = useRef<HTMLDivElement>(null);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const firstRender = useRef(true);

  const form = useForm<NewClaimValues>({
    defaultValues: {
      policyId: '',
      claimType: '',
      incidentDate: '',
      amount: '',
      description: '',
      files: [],
      confirmed: false,
    },
    shouldFocusError: false,
  });
  const { register, setValue, getValues, formState } = form;
  const errors = formState.errors;

  const policyOf = (id: string) => policies.data?.find((policy) => policy.policyId === id);
  // Every field's current value. useWatch re-draws this page when one changes, so the limit, the character
  // count and the review always show what was just typed.
  const values = useWatch({ control: form.control }) as NewClaimValues;
  const policy = policyOf(values.policyId);
  const limit = values.claimType && policy ? policy.limits[values.claimType] : undefined;
  const typeLabel = values.claimType ? claimTypeLabel[values.claimType] : '';

  // With one policy there is nothing to choose; pick it.
  useEffect(() => {
    if (policies.data?.length === 1 && !getValues('policyId')) {
      setValue('policyId', policies.data[0].policyId);
    }
  }, [policies.data, getValues, setValue]);

  // The files aren't typed into an input, so their rule is registered here rather than on an element.
  useEffect(() => {
    register('files', { validate: (files) => checkFiles(files) ?? true });
  }, [register]);

  // A new step starts at the top, with focus on its heading, so a screen reader announces where it is.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    window.scrollTo({ top: 0 });
    headingRef.current?.focus();
  }, [step]);

  /** Shows the error summary and moves focus to it, once React has drawn it. */
  function showProblems() {
    setShowSummary(true);
    requestAnimationFrame(() => summaryRef.current?.focus());
  }

  async function next() {
    const ok = await form.trigger(stepFields[step]);
    if (!ok) {
      showProblems();
      return;
    }
    setShowSummary(false);
    setStep(step + 1);
  }

  function goTo(target: number) {
    setShowSummary(false);
    setStep(target);
  }

  const submit = useMutation({
    mutationFn: (submitted: NewClaimValues) => {
      const claim = toSubmitRequest(submitted);
      return unwrap(
        api.POST('/api/claims', {
          body: { claim },
          // The typed body is the JSON part; the real request is multipart with the files beside it.
          bodySerializer: () => toFormData(claim, submitted.files),
        }),
      );
    },
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: myClaimsQueryKey });
      void queryClient.invalidateQueries({ queryKey: inboxQueryKey });
      void navigate('/claims', { state: { submitted: created.reference } });
    },
  });

  const summaryErrors: SummaryError[] = showSummary
    ? stepFields[step]
        .filter((name) => errors[name]?.message)
        .map((name) => ({ fieldId: fieldId[name], message: String(errors[name]?.message) }))
    : [];

  if (policies.isPending) return <SectionLoading label="Loading your policies…" />;
  if (policies.isError) return <PageError message={policies.error.message} onRetry={() => void policies.refetch()} />;
  if (policies.data.length === 0) {
    return (
      <div className={page.page}>
        <title>New claim · Teduh Claims</title>
        <h1>New claim</h1>
        <p className={page.quiet}>You need a Teduh policy to make a claim.</p>
      </div>
    );
  }

  return (
    <div className={page.page}>
      <title>{`New claim, step ${step + 1} of 4 · Teduh Claims`}</title>
      <div className={styles.top}>
        <h1 className={styles.pageTitle}>New claim</h1>
        <Stepper current={step + 1} steps={stepNames} />
      </div>

      <form
        className={styles.form}
        noValidate
        onSubmit={(event) => void form.handleSubmit((valid) => submit.mutate(valid), showProblems)(event)}
      >
        <ErrorSummary ref={summaryRef} errors={summaryErrors} />

        {step === 0 && (
          <>
            <fieldset className={styles.group}>
              <legend>
                <h2 ref={headingRef} tabIndex={-1}>
                  Which policy is this for?
                </h2>
              </legend>
              {policies.data.map((option, index) => (
                <ChoiceCard
                  key={option.policyId}
                  id={index === 0 ? fieldId.policyId : undefined}
                  value={option.policyId}
                  title={option.planName}
                  aside={coverState(option, today) === 'active' ? 'Active' : 'Not active'}
                  description={`Policy ${option.policyNumber}. Covered ${formatDate(option.coverStart)} to ${formatDate(option.coverEnd)}.`}
                  icon={<Plane size={20} />}
                  aria-invalid={errors.policyId ? true : undefined}
                  {...register('policyId', { required: 'Choose the policy this claim is under.' })}
                />
              ))}
              {errors.policyId && <p className={styles.error}>{errors.policyId.message}</p>}
            </fieldset>

            <fieldset className={styles.group}>
              <legend>
                <h2>What happened?</h2>
              </legend>
              {(policy ? limitsOf(policy) : []).map((option, index) => (
                <ChoiceCard
                  key={option.type}
                  id={index === 0 ? fieldId.claimType : undefined}
                  value={option.type}
                  title={option.label}
                  aside={`Up to ${formatMoney(option.limit)}`}
                  description={typeDescription[option.type]}
                  icon={typeIcon[option.type]}
                  aria-invalid={errors.claimType ? true : undefined}
                  {...register('claimType', { validate: (type) => (type ? true : 'Choose what happened.') })}
                />
              ))}
              {!policy && <p className={page.quiet}>Choose a policy first.</p>}
              {errors.claimType && <p className={styles.error}>{errors.claimType.message}</p>}
            </fieldset>
          </>
        )}

        {step === 1 && policy && (
          <>
            <div className={styles.intro}>
              <h2 ref={headingRef} tabIndex={-1}>
                Tell us what happened
              </h2>
              <p>
                {typeLabel} under {policy.planName} ({policy.policyNumber}).
              </p>
            </div>
            <TextField
              id={fieldId.incidentDate}
              label="When did it happen?"
              type="date"
              min={policy.coverStart}
              max={latestIncidentDate(policy, today)}
              hint={`Between ${formatDate(policy.coverStart)} and ${formatDate(latestIncidentDate(policy, today))}.`}
              error={errors.incidentDate?.message}
              {...register('incidentDate', {
                validate: (date) => checkIncidentDate(date, policyOf(getValues('policyId')), today) ?? true,
              })}
            />
            <TextField
              id={fieldId.amount}
              label="How much are you claiming?"
              inputMode="decimal"
              autoComplete="off"
              icon={<span className={styles.currency}>RM</span>}
              hint={limit !== undefined ? `Your limit for ${typeLabel.toLowerCase()} is ${formatMoney(limit)}.` : undefined}
              error={errors.amount?.message}
              {...register('amount', {
                validate: (amount) => checkAmount(amount, limit, typeLabel.toLowerCase()) ?? true,
              })}
            />
            <TextArea
              id={fieldId.description}
              label="What happened?"
              hint={`Say what went wrong and what you had to spend. Up to ${MAX_DESCRIPTION} characters.`}
              count={{ used: values.description.length, max: MAX_DESCRIPTION }}
              error={errors.description?.message}
              {...register('description', { validate: (text) => checkDescription(text) ?? true })}
            />
          </>
        )}

        {step === 2 && (
          <>
            <div className={styles.intro}>
              <h2 ref={headingRef} tabIndex={-1}>
                Your receipts and proof
              </h2>
              <p>
                Attach the receipts for what you're claiming, and any report from the airline. Up to {MAX_FILES}{' '}
                files.
              </p>
            </div>
            <FileDrop
              id={fieldId.files}
              max={MAX_FILES}
              files={values.files.map((picked) => ({
                id: picked.id,
                name: picked.file.name,
                size: picked.file.size,
                problem: picked.problem,
              }))}
              error={errors.files?.message}
              onAdd={(added) =>
                setValue(
                  'files',
                  [
                    ...getValues('files'),
                    ...added.map((file) => ({ id: crypto.randomUUID(), file, problem: checkFile(file) })),
                  ],
                  { shouldValidate: Boolean(errors.files) },
                )
              }
              onRemove={(id) =>
                setValue(
                  'files',
                  getValues('files').filter((picked) => picked.id !== id),
                  { shouldValidate: Boolean(errors.files) },
                )
              }
            />
          </>
        )}

        {step === 3 && policy && (
          <Review
            values={values}
            policy={policy}
            headingRef={headingRef}
            onEdit={goTo}
            today={today}
          />
        )}

        {step === 3 && (
          <label className={styles.confirm}>
            <input
              id={fieldId.confirmed}
              type="checkbox"
              aria-invalid={errors.confirmed ? true : undefined}
              aria-describedby={errors.confirmed ? 'confirm-error' : undefined}
              {...register('confirmed', {
                validate: (ticked) => ticked || 'Tick the box to confirm the details are correct.',
              })}
            />
            <span>I confirm these details are correct and the documents are for this claim.</span>
          </label>
        )}
        {step === 3 && errors.confirmed && (
          <p className={styles.error} id="confirm-error">
            {errors.confirmed.message}
          </p>
        )}

        {step === 3 && submit.isError && (
          <Banner variant="error" title="Your claim wasn't sent">
            {submit.error.message}
          </Banner>
        )}

        <div className={styles.actions}>
          {step > 0 && (
            <Button variant="secondary" icon={<ArrowLeft size={16} aria-hidden="true" />} onClick={() => goTo(step - 1)}>
              Back
            </Button>
          )}
          {step < 3 ? (
            <Button className={styles.forward} onClick={() => void next()}>
              Continue
              <ArrowRight size={16} aria-hidden="true" />
            </Button>
          ) : (
            <Button
              type="submit"
              className={styles.forward}
              loading={submit.isPending}
              icon={<Send size={16} aria-hidden="true" />}
            >
              Submit claim
            </Button>
          )}
        </div>
      </form>
    </div>
  );
}

type ReviewProps = {
  values: NewClaimValues;
  policy: Policy;
  headingRef: RefObject<HTMLHeadingElement | null>;
  onEdit: (step: number) => void;
  today: string;
};

// C06: everything the customer entered, with an Edit button per part that goes back to that step.
function Review({ values, policy, headingRef, onEdit, today }: ReviewProps) {
  const type = values.claimType as ClaimType;
  return (
    <>
      <div className={styles.intro}>
        <h2 ref={headingRef} tabIndex={-1}>
          Check your details
        </h2>
        <p>Make sure everything is right before you submit. You can't change a claim after it's sent.</p>
      </div>

      <section className={page.panel} aria-labelledby="review-policy">
        <ReviewTitle id="review-policy" title="Policy and claim type" onEdit={() => onEdit(0)} />
        <dl className={page.facts}>
          <div>
            <dt>Policy</dt>
            <dd>
              {policy.planName} ({policy.policyNumber}){coverState(policy, today) === 'active' ? '' : ', not active'}
            </dd>
          </div>
          <div>
            <dt>What happened</dt>
            <dd>{claimTypeLabel[type]}</dd>
          </div>
        </dl>
      </section>

      <section className={page.panel} aria-labelledby="review-details">
        <ReviewTitle id="review-details" title="Incident details" onEdit={() => onEdit(1)} />
        <dl className={page.facts}>
          <div>
            <dt>Date</dt>
            <dd>{values.incidentDate ? formatDate(values.incidentDate) : ''}</dd>
          </div>
          <div>
            <dt>Amount claimed</dt>
            <dd>{formatMoney(Number(values.amount))}</dd>
          </div>
        </dl>
        <blockquote className={styles.quote}>{values.description}</blockquote>
      </section>

      <section className={page.panel} aria-labelledby="review-files">
        <ReviewTitle id="review-files" title={`Documents (${values.files.length})`} onEdit={() => onEdit(2)} />
        <ul className={styles.fileList}>
          {values.files.map((picked) => (
            <li key={picked.id}>
              <span>{picked.file.name}</span>
              <span>{formatFileSize(picked.file.size)}</span>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}

function ReviewTitle({ id, title, onEdit }: { id: string; title: string; onEdit: () => void }) {
  return (
    <div className={styles.reviewTitle}>
      <h3 id={id}>{title}</h3>
      <button type="button" className={styles.edit} onClick={onEdit}>
        <Pencil size={14} aria-hidden="true" />
        Edit<span className="visually-hidden">{` ${title.toLowerCase()}`}</span>
      </button>
    </div>
  );
}
