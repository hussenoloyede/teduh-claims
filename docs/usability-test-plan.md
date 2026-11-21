# Usability test plan

Teduh Claims, week 7. Written before the first session, so the findings can be compared with what was planned.

## What we want to learn

1. Can someone with no cover yet choose a plan for a trip, and understand that nothing is charged?
2. Can a traveller file a claim with documents, follow it, and answer an agent's request without help?
3. Can someone who has never seen the agent screens pick up a claim, ask for a document, and approve less than was claimed, with a reason?
4. Which words, labels or steps make people hesitate or go the wrong way?

The test checks clarity. The participants are classmates, not real travellers mid-claim or trained claims officers, so the findings on the agent tasks are about whether the screens are clear, not about how a real claims team works. The write-up says so.

## Participants

- Three classmates who have taken a flight. Jakob Nielsen's "Why You Only Need to Test with 5 Users" (Nielsen Norman Group, 2000) suggests five; three is what the deadline allows. On the same article's curve, three people find about two thirds of the problems, against about 85% for five, so the write-up says three were tested and that some problems may have been missed.
- Nobody who has seen Teduh before or helped build it.
- Called P1 to P3 in every note. No names, faces or contact details are written down.

## Setup

- One laptop, the live site: https://teduh-claims.grayisland-5bebc3bc.eastasia.azurecontainerapps.io
- Two browsers side by side, because one browser can only be signed in as one person at a time:
  - Customer window: Chrome, made narrow (about 400 px wide) so it looks like a phone. It starts on the sign-in page for task 1, then is signed in as Aisyah with "Try as customer" for tasks 2 to 7.
  - Agent window: a second browser (Edge, or a Chrome guest window), full width. Signed in as Farid with "Try as agent".
- The three test documents in a folder on the desktop called `Teduh test files` (from `docs/usability-test-files/` in the repo):
  - `receipt-clothes.pdf`: clothes, RM 289.00, 10 Nov 2026
  - `receipt-toiletries.pdf`: toiletries, RM 111.00, 10 Nov 2026
  - `airline-delay-letter.pdf`: the bag was delayed, landed 10 Nov 2026, delivered 12 Nov 2026

  All three are made up, say so on the page, and name no real shop or airline.
- This plan printed, and one copy of the record sheet (at the end) per participant.
- A phone or watch to time each task.

## Before each session (about 5 minutes, before the participant arrives)

1. Open the readiness page (`/actuator/health/readiness`) and wait until it shows UP. The demo server sleeps when idle and takes about a minute to wake. Its first start each day also clears the demo (all claims, and the accounts made with "Try as a new customer"), so the first session of the day starts from a clean demo.
2. In the agent window, as Farid: open Claims queue. If a claim from an earlier session is still under review or waiting on the customer (see Assigned reviews), finish it so it doesn't confuse this session.
3. In both windows: open Notifications and press "Mark all as read", so the bell starts at zero.
4. Sign the customer window out (Profile, Sign out), so it shows the sign-in page. Leave the agent window on the Claims queue.

## The session (about 35 minutes)

### Welcome and consent (3 minutes)

Read this out:

> Thanks for helping. I'm testing a website, not you: if something is hard, that's the website's fault, and it's exactly what I need to find. Teduh is a made-up travel insurer I built for my portfolio. Nothing is real: no money, no real documents.
>
> I'll ask you to do seven short tasks, playing three people: a traveller about to leave on a trip, a traveller called Aisyah, and a claims officer called Farid. Please think aloud as you go: what you're looking for, what you expect, what surprises you. I can't help during a task, but I'll answer any question afterwards.
>
> I'll take written notes and time each task. I won't record your face, your voice or your screen, and your name won't appear anywhere. You can stop at any time without giving a reason. Is that OK?

Only continue on a clear yes.

### Warm-up (2 minutes)

In the customer window, on the sign-in page: "Take a minute to look at this page and tell me what you think this site is for." Don't let them press anything yet. Don't time it.

### Tasks (about 25 minutes)

Hand over each task on its own card, in this order. Task 1 is a new customer getting cover. Tasks 2 to 7 follow one claim through its life, switching between Aisyah and Farid as the claim moves, so every task has something to act on.

Between task 1 and task 2, sign the customer window out yourself and press "Try as customer" (Aisyah). Aisyah's policy has covered her since October, so her claim for 10 November is inside it; the new customer's cover only starts today, so it can't be used for the receipts dated 10 November.

After each task, ask the ease question: "On a scale of 1 to 7, where 1 is very hard and 7 is very easy, how easy was that?"

A task ends when the participant says they're done, gives up, or reaches 5 minutes. Say "Thanks, let's move on" and don't show them the answer until the end.

| # | Window | Task card | Done when |
| --- | --- | --- | --- |
| 1 | Customer | You leave today for a 10-day holiday and you don't have travel cover yet. Start as a new customer and get covered for the whole trip. Choose whichever plan you think fits. | The page says "You're covered", with the trip dates the participant meant (today to 9 days from today). Note which plan they chose and anything they said about the price. |
| 2 | Customer | You're now Aisyah, who already has a Teduh policy. You landed in Kuala Lumpur on 10 November, but your checked bag arrived two days late. You bought clothes and toiletries while you waited, and spent RM 50 on meals without receipts. Claim RM 450 on your travel policy. Your two receipts are in the `Teduh test files` folder on the desktop. | The claim is submitted. Write down its reference (CLM-2026-…) for task 4. |
| 3 | Customer | A few minutes later, Aisyah wonders what's happening with her claim. Find out where it's up to and what happens next. | They say it's waiting for an agent (Submitted) and that someone will review it. |
| 4 | Agent | You're now Farid, a claims officer at Teduh. Start reviewing Aisyah's claim [write the reference from task 2 here]. | The claim shows Under review and Assigned to you. |
| 5 | Agent | The receipts show what Aisyah bought, but nothing proves the bag was late. Ask her for the airline's delay letter. | The claim shows More info needed. |
| 6 | Customer | You're Aisyah again. Teduh needs something from you. Provide it. The file is in the `Teduh test files` folder. | The claim is back to Under review with the delay letter attached. |
| 7 | Agent | You're Farid. The receipts add up to RM 400, and the RM 50 of meals has no receipt. Approve the claim for RM 400 instead of RM 450. | The claim shows Approved, RM 400, with a reason. |

### Debrief (5 minutes)

1. What was the easiest part? What was the most confusing?
2. When you chose a plan, did you think you would be charged? What made you think so?
3. As Aisyah, was there a moment you weren't sure what was going on with your claim?
4. As Farid, did you feel you had what you needed to decide?
5. Is there anything you expected to see that wasn't there?

Then show them anything they got stuck on, answer their questions, and thank them.

## What to record

For each task, on the record sheet:

- Completed: yes, with help, or no. "With help" means you had to say something that pointed them to the answer, which counts as not completed in the results.
- Time, from handing over the card to "done", in seconds.
- Errors: wrong page, wrong button, a field error, going back.
- Hesitations: more than about 5 seconds of searching, or "hmm, where…".
- What they said, as close to word for word as possible, especially surprise or confusion.
- The ease score, 1 to 7.

Write what happened, not why. "Clicked Policies looking for the claim" is useful; "didn't understand navigation" is a guess.

## After each session (10 minutes, the same day)

1. Copy the record sheet into `docs/usability-findings.md`, under that participant.
2. List each problem you saw, with the task and what happened.
3. In the agent window, check that the participant's claim ended Approved. If a task stopped early, finish the claim as Farid so the next session starts clean.

## After all three sessions

Rate each problem once, across all sessions:

| Severity | Meaning |
| --- | --- |
| 4 | Blocks the task: they couldn't finish without help |
| 3 | Major: they finished, but with real difficulty or an error |
| 2 | Minor: hesitation or a small detour |
| 1 | Cosmetic: noticed, but no effect on the task |

Fix every 3 and 4, take before and after screenshots of each fix, and note which findings were left as they are and why. The case study uses the numbers: completion per task, median time, median ease score, and the problems by severity.

## Record sheet (one per participant)

Participant: P__ Date: ________ Start time: ______

| # | Completed (yes / with help / no) | Time (s) | Errors | Hesitations | What they said | Ease (1-7) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |
| 5 | | | | | | |
| 6 | | | | | | |
| 7 | | | | | | |

Debrief answers:

1.
2.
3.
4.
5.
