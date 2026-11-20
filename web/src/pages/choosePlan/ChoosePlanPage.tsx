import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, CalendarDays, CircleCheck, ShieldCheck } from 'lucide-react';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { claimTypeLabel, type ClaimType } from '../../api/claims';
import { api, unwrap } from '../../api/client';
import { shortPlanName, usePlans, type PlanOffer, type TravelPlan } from '../../api/plans';
import { coverState, limitsOf, policiesQueryKey, type Policy } from '../../api/policies';
import { Banner } from '../../components/Banner/Banner';
import { Button, ButtonLink } from '../../components/Button/Button';
import { PageError, SectionLoading } from '../../components/PageStatus/PageStatus';
import { PlanCard } from '../../components/PlanCard/PlanCard';
import { PolicyCard } from '../../components/PolicyCard/PolicyCard';
import { Stepper } from '../../components/Stepper/Stepper';
import { TextField } from '../../components/TextField/TextField';
import { formatDate, formatPlanMoney, todayInMalaysia } from '../../lib/format';
import { queryClient } from '../../queryClient';
import page from '../Page.module.css';
import {
  addDays,
  checkTripDates,
  formatDays,
  formatTrip,
  latestStart,
  MAX_TRIP_DAYS,
  tripDays,
  type TripDateErrors,
} from './choosePlanForm';
import styles from './ChoosePlanPage.module.css';

const stepNames = ['Plan', 'Trip dates', 'Confirm'];

/** The plan's limits with their labels, in the same order as everywhere else in the app. */
function limitRows(plan: PlanOffer) {
  return (Object.keys(claimTypeLabel) as ClaimType[])
    .filter((type) => plan.limits[type] !== undefined)
    .map((type) => ({ label: claimTypeLabel[type], limit: plan.limits[type] }));
}

// /plans: C16 to C18, choosing a plan for a trip in three steps, then C19 once the policy exists. There is
// no payment step: Teduh is a demo, the prices are labelled as demo prices, and nothing is charged. The
// policy itself is real inside the app, so the customer can claim on it once the cover starts.
//
// The server checks the dates again (PolicySales.java) and also refuses a trip that overlaps one the
// customer already has cover for. That one is only known to the server, so it shows on the last step.
export function ChoosePlanPage() {
  const plans = usePlans();
  const today = todayInMalaysia();
  const [step, setStep] = useState(0);
  // Plus is the plan most people pick, so it starts chosen, as in the design. Nothing is charged either way.
  const [planId, setPlanId] = useState<TravelPlan>('PLUS');
  // A week from today, which the customer will usually change.
  const [start, setStart] = useState(today);
  const [end, setEnd] = useState(addDays(today, 6));
  const [dateErrors, setDateErrors] = useState<TripDateErrors>({});
  const [checkedDates, setCheckedDates] = useState(false);
  const [understood, setUnderstood] = useState(false);
  const [understoodError, setUnderstoodError] = useState(false);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const firstRender = useRef(true);

  const plan = plans.data?.find((offer) => offer.plan === planId);

  // A new step starts at the top, with focus on its heading, so a screen reader announces where it is.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    window.scrollTo({ top: 0 });
    headingRef.current?.focus();
  }, [step]);

  const buy = useMutation({
    mutationFn: () =>
      unwrap(api.POST('/api/policies', { body: { plan: planId, coverStart: start, coverEnd: end } })),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: policiesQueryKey }),
  });

  function changeDate(which: 'start' | 'end', value: string) {
    const nextStart = which === 'start' ? value : start;
    const nextEnd = which === 'end' ? value : end;
    if (which === 'start') setStart(value);
    else setEnd(value);
    // Once the customer has pressed Continue, the messages follow what they type.
    if (checkedDates) setDateErrors(checkTripDates(nextStart, nextEnd, today));
  }

  function continueFromDates() {
    const errors = checkTripDates(start, end, today);
    setDateErrors(errors);
    setCheckedDates(true);
    if (errors.start) {
      document.getElementById('trip-start')?.focus();
    } else if (errors.end) {
      document.getElementById('trip-end')?.focus();
    } else {
      setStep(2);
    }
  }

  function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!understood) {
      setUnderstoodError(true);
      document.getElementById('trip-understood')?.focus();
      return;
    }
    buy.mutate();
  }

  if (buy.data) return <Covered policy={buy.data} today={today} />;
  if (plans.isPending) return <SectionLoading label="Loading the plans…" />;
  if (plans.isError) return <PageError message={plans.error.message} onRetry={() => void plans.refetch()} />;
  if (!plan) return null;

  const currentErrors = checkTripDates(start, end, today);
  const days = currentErrors.start || currentErrors.end ? undefined : tripDays(start, end);

  return (
    <div className={page.page}>
      <title>{`Choose a plan, step ${step + 1} of 3 · Teduh Claims`}</title>
      <div className={styles.top}>
        {step === 0 && (
          <Link to="/" className={styles.back}>
            <ArrowLeft size={16} aria-hidden="true" />
            Home
          </Link>
        )}
        <Stepper current={step + 1} steps={stepNames} label="Choosing a plan" />
      </div>

      {step === 0 && (
        <div className={styles.form}>
          <fieldset className={styles.group}>
            <legend className={styles.intro}>
              <h1 ref={headingRef} tabIndex={-1}>
                Choose a plan
              </h1>
              <span>Every plan covers the same four things. Higher plans pay more per claim.</span>
            </legend>
            {plans.data.map((offer) => (
              <PlanCard
                key={offer.plan}
                name="plan"
                value={offer.plan}
                checked={offer.plan === planId}
                onChange={() => setPlanId(offer.plan)}
                title={offer.name}
                blurb={offer.blurb}
                price={offer.pricePerTrip}
                limits={limitRows(offer)}
              />
            ))}
          </fieldset>
          <Banner title="Demo prices">Teduh is a portfolio project, so nothing is ever charged.</Banner>
          <Button fullWidth onClick={() => setStep(1)}>
            Continue with {shortPlanName(plan.name)}
            <ArrowRight size={16} aria-hidden="true" />
          </Button>
        </div>
      )}

      {step === 1 && (
        <div className={styles.form}>
          <div className={styles.intro}>
            <h1 ref={headingRef} tabIndex={-1}>
              When is your trip?
            </h1>
            <p>Cover runs from the day you leave to the day you get home, up to {MAX_TRIP_DAYS} days.</p>
          </div>
          <TextField
            id="trip-start"
            label="Leaving on"
            type="date"
            min={today}
            max={latestStart(today)}
            hint="Today or later."
            icon={<CalendarDays size={18} />}
            value={start}
            onChange={(event) => changeDate('start', event.target.value)}
            error={dateErrors.start}
          />
          <TextField
            id="trip-end"
            label="Back on"
            type="date"
            min={start || today}
            max={start ? addDays(start, MAX_TRIP_DAYS - 1) : undefined}
            icon={<CalendarDays size={18} />}
            value={end}
            onChange={(event) => changeDate('end', event.target.value)}
            error={dateErrors.end}
          />
          <div className={styles.summary} aria-live="polite">
            {days !== undefined && (
              <>
                <CalendarDays size={20} aria-hidden="true" />
                <p>
                  <strong>{formatDays(days)} of cover</strong>
                  <span>
                    {plan.name}, {formatTrip(start, end)}
                  </span>
                </p>
              </>
            )}
          </div>
          <div className={styles.actions}>
            <Button variant="secondary" icon={<ArrowLeft size={16} aria-hidden="true" />} onClick={() => setStep(0)}>
              Back
            </Button>
            <Button className={styles.forward} onClick={continueFromDates}>
              Continue
              <ArrowRight size={16} aria-hidden="true" />
            </Button>
          </div>
        </div>
      )}

      {step === 2 && days !== undefined && (
        <form className={styles.form} noValidate onSubmit={confirm}>
          <div className={styles.intro}>
            <h1 ref={headingRef} tabIndex={-1}>
              Check and confirm
            </h1>
          </div>

          <section className={page.panel} aria-label="Your plan and trip">
            <dl className={styles.rows}>
              <div>
                <dt>Plan</dt>
                <dd>{plan.name}</dd>
              </div>
              <div>
                <dt>Cover</dt>
                <dd>{formatTrip(start, end)}</dd>
              </div>
              <div>
                <dt>Length</dt>
                <dd>{formatDays(days)}</dd>
              </div>
            </dl>
          </section>

          <section className={page.panel} aria-labelledby="confirm-limits">
            <h2 id="confirm-limits" className={styles.overline}>
              Most Teduh pays per claim
            </h2>
            <dl className={styles.rows}>
              {limitRows(plan).map((row) => (
                <div key={row.label}>
                  <dt>{row.label}</dt>
                  <dd>{formatPlanMoney(row.limit)}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className={`${page.panel} ${styles.price}`} aria-label="Price">
            <p>
              <strong>Price</strong>
              <span>Demo price, not charged</span>
            </p>
            <p className={styles.amount}>{formatPlanMoney(plan.pricePerTrip)}</p>
          </section>

          <label className={styles.confirm}>
            <input
              id="trip-understood"
              type="checkbox"
              checked={understood}
              onChange={(event) => {
                setUnderstood(event.target.checked);
                if (event.target.checked) setUnderstoodError(false);
              }}
              aria-invalid={understoodError || undefined}
              aria-describedby={understoodError ? 'trip-understood-error' : undefined}
            />
            <span>I understand Teduh is a demo. This policy lets me try claims; it is not real travel cover.</span>
          </label>
          {understoodError && (
            <p className={styles.error} id="trip-understood-error">
              Tick the box to show you understand this is a demo.
            </p>
          )}

          {buy.isError && (
            <Banner
              variant="error"
              title="Your policy wasn't made"
              action={
                <Button variant="secondary" size="sm" onClick={() => setStep(1)}>
                  Change the dates
                </Button>
              }
            >
              {buy.error.message}
            </Banner>
          )}

          <div className={styles.actions}>
            <Button variant="secondary" icon={<ArrowLeft size={16} aria-hidden="true" />} onClick={() => setStep(1)}>
              Back
            </Button>
            <Button
              type="submit"
              className={styles.forward}
              loading={buy.isPending}
              icon={<ShieldCheck size={16} aria-hidden="true" />}
            >
              Get covered
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}

// C19: the new policy, as the home page will show it, and when claims for it can start.
function Covered({ policy, today }: { policy: Policy; today: string }) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    window.scrollTo({ top: 0 });
    headingRef.current?.focus();
  }, []);
  const trip = formatTrip(policy.coverStart, policy.coverEnd);
  const startsToday = policy.coverStart <= today;

  return (
    <div className={page.page}>
      <title>You're covered · Teduh Claims</title>
      <div className={styles.done}>
        <span className={styles.doneIcon} aria-hidden="true">
          <CircleCheck size={32} />
        </span>
        <h1 ref={headingRef} tabIndex={-1}>
          You're covered
        </h1>
        <p>
          Your {policy.planName} policy runs from {trip}.
        </p>
      </div>
      <PolicyCard
        planName={policy.planName}
        policyNumber={policy.policyNumber}
        coverStart={policy.coverStart}
        coverEnd={policy.coverEnd}
        state={coverState(policy, today)}
        covers={limitsOf(policy).map((limit) => limit.label)}
      />
      <Banner title="Claiming">
        {startsToday
          ? 'You can claim for anything that happens while your cover runs, starting today.'
          : `You can claim for anything that happens while your cover runs. Claims for ${formatDate(policy.coverStart)} onwards can be made from that day.`}
      </Banner>
      <ButtonLink to="/" fullWidth>
        Go to home
      </ButtonLink>
    </div>
  );
}
