# Teduh Claims MVP scope

Project 4 of the SWE internship portfolio plan, started before project 1. A claims portal for Teduh, a fictional travel insurer. Customers submit a claim with supporting documents, an agent reviews it and decides, and both sides see the status and get notified at each step.

It is also the design-process showcase: Figma flows, a component library in Storybook, and a usability test with five classmates, written up as a case study.

Status: scope agreed 7 November 2026. Week 1 setup is done the same day: Docker works on the PC, Java 25 is set up for this project, and the generated project builds and passes its first Testcontainers test (commit `69a7fff`). Week 2 (claim state machine, database, policies, claims API) was done the same day, and week 3 (accounts and demo data, real sign-in with roles, ownership and CSRF, documents in Blob Storage) on 8 November, with 114 tests passing. Week 4 (notifications, Docker image, Azure resources and the Azure DevOps pipeline) was done on 10 November: 121 tests pass and the API is live at https://teduh-claims.grayisland-5bebc3bc.eastasia.azurecontainerapps.io, deployed on every push to main. Week 5 (design system) is under way: Hussein's Hi-Fi 2.0 screens and the Figma variables are done, and `web/` holds the React app with the design tokens and ten shared components in Storybook, 47 frontend tests passing (commit `edeb71f`). The companion doc `claude/Teduh Claims project guide.md` explains how each part works and why.

## The demo story

The MVP is finished when this demo works end to end on the live link.

1. On a phone-width screen, tap "Try as customer". You are Aisyah. Her home page shows her Teduh Travel Plus policy and no claims yet.
2. Start a claim: pick the policy, choose "Baggage delayed", enter the incident date and RM 450, describe what happened, and attach two receipts (PDF or photo). Check the summary and submit. She gets a reference such as `CLM-2026-000017` and a notification that it was received.
3. On a laptop, tap "Try as agent". You are Farid. His queue lists the new claim as Submitted, oldest first. He opens it, looks at the receipts in the page, and starts the review. The claim is now assigned to him and Aisyah is notified.
4. Farid asks for the airline's delay report. The claim moves to More info needed.
5. Back as Aisyah, the bell shows one unread notification. The claim page says exactly what Farid asked for. She uploads the report with a short note, and the claim goes back to Under review. Farid is notified.
6. Farid approves RM 400 of the RM 450 and gives the reason ("Receipts total RM 400"). Aisyah sees Approved, the amount, the reason, and a timeline of every step with who did it and when.
7. Show the guards. Aisyah opening another customer's claim by URL gets "Claim not found". A second agent who tries to act on Farid's claim is refused, and two agents acting on the same claim at once get "This claim changed while you were looking at it. Reload to see the latest."

Every screen in that story is built from components in the Storybook library, and the case study shows what changed after the usability test.

## The claim state machine

```
SUBMITTED ──start review──▶ UNDER_REVIEW ──approve──▶ APPROVED
                              │      ▲    └──reject───▶ REJECTED
                 request info │      │ customer responds
                              ▼      │
                          MORE_INFO_NEEDED
```

- A claim is created in `SUBMITTED`, already carrying at least one document. There is no draft state.
- Any agent can start the review of a `SUBMITTED` claim. Starting assigns the claim to that agent, and from then on only that agent can act on it.
- Requesting info needs a message (up to 1,000 characters). There can be several rounds.
- The customer responds with a note, at least one file, or both. The claim returns to `UNDER_REVIEW` with the same agent.
- Approving needs an amount above zero and no higher than the amount claimed. If it is lower than claimed, a reason is required.
- Rejecting always needs a reason, and the customer sees it.
- `APPROVED` and `REJECTED` are final.
- Every transition is recorded as a claim event: from, to, who, when, and the message or reason.
- Breaking a rule returns `422` with a problem-details body. Acting on a claim that someone else changed first returns `409`.

## In scope

### Accounts and policies (supporting modules)
- Seeded demo users only: two customers and two agents, with `@example.com` addresses. Passwords stored as BCrypt hashes.
- The sign-in page has "Try as customer" and "Try as agent" buttons that sign in to the seeded accounts for real. A normal email and password form sits below them.
- Each customer has one or two travel policies: policy number, plan name, cover start and end, and a limit per claim type. The seeder sets cover dates relative to today so the demo never expires.
- Claim types: Flight delay, Baggage delayed, Lost or damaged baggage, Trip cancellation. No medical claims, so no health data, even fictional.

### Claims module
- Submit a claim (customer): policy, type, incident date, amount in MYR, description, and 1 to 5 files, all in one request. The policy must be the customer's, the incident date must fall inside the cover period and not be in the future, and the amount must not exceed the limit for that claim type.
- Customer list of their own claims, and claim detail with status, "what happens next" text, documents and timeline.
- Agent queue with filters (Submitted, In review by me, Waiting on customer, Decided), sorted oldest first, with the age of each claim.
- The transitions above, each as its own endpoint.
- Optimistic locking with a version column, so two agents can't both act on the same claim.
- Customers who ask for someone else's claim get `404`, the same as a claim that doesn't exist.

### Documents module
- PDF, JPEG and PNG only, checked by the file's first bytes, not only by the name or the browser's content type.
- Up to 10 MB per file, 5 files per upload, 15 per claim.
- Stored in a private Blob Storage container; Azurite locally. SHA-256 and size recorded with each file.
- Read-only download links that expire after 15 minutes, given out only after checking the user may see that claim. Agents see image previews in the page through the same links.
- Files are never overwritten or edited.

### Notifications module
- In-app only. A notification is stored for the right person when a claim is submitted, a review starts, info is requested, the customer responds, and a claim is approved or rejected.
- The claims module publishes events; the notifications module listens. The two never call each other.
- A bell with an unread count, a list, mark one read, mark all read. The browser checks for new ones every 30 seconds.

### Frontend
- React with TypeScript and Vite, built from Hussein's Figma designs, served by the Spring Boot app on the same domain.
- Customer screens: sign-in, home, new claim (four steps), claim detail with the respond form, notifications.
- Agent screens: queue, claim review with document previews and the action panel, notifications.
- Customer screens designed phone-first; agent screens designed for a laptop and still usable on a tablet.
- A footer on every page: "Teduh is a fictional insurer made for a portfolio project. Don't upload real documents."
- Aim for WCAG 2.2 AA: keyboard use, visible focus, labelled fields, an error summary on forms, and status never shown by colour alone.

### Design process
- Figma: user flows for both roles, wireframes, high-fidelity screens, variables for colour, type and spacing, and components with variants that match the code.
- Storybook: every shared component with its states, the accessibility checks on, and stories run as tests. Published with the app at `/storybook/`.
- Usability test: five classmates, six tasks across both roles, on the deployed app. Findings rated by severity, changes made, before and after screenshots.
- A case study page (README section first, portfolio site later) covering problem, flows, components, test findings and what changed.

### Engineering
- Java 25, Spring Boot 4 (started on 4.1.2), Spring Modulith 2.1, Maven through the Maven wrapper, Spring Data JPA, Flyway, PostgreSQL.
- Unit tests for the state machine with no Spring involved; a Modulith test that fails if one module reaches into another's internals; integration tests over HTTP against real PostgreSQL and Azurite through Testcontainers; security tests for every role and ownership rule.
- Vitest and Testing Library for screens; Storybook stories with accessibility checks run in the same test run.
- TypeScript types generated from the API's OpenAPI document, so a changed endpoint breaks the frontend build instead of the live site.
- Azure DevOps pipeline: build, all tests, image build, push to the container registry, deploy to Azure Container Apps, on every push to main.
- Azure: Container Apps (scale to zero), PostgreSQL Flexible Server on the free B1MS offer, Blob Storage, and a public image on GitHub Container Registry. The app reaches the database and storage with its managed identity; no passwords or keys in the app. The only secret anywhere is the pipeline's GitHub token for pushing images.
- Demo data resets itself: on startup, if the last reset was more than 24 hours ago, claims, documents, notifications and events are cleared and the seed data reloaded.

## Out of scope for the MVP

Good "What I'd do next" README items.

- Self sign-up, password reset and email verification.
- Keycloak or Microsoft Entra External ID in place of built-in accounts.
- Email or SMS notifications (Azure Communication Services). The MVP has in-app notifications only.
- Splitting the modules into separate services talking over RabbitMQ or Kafka. Spring Modulith can publish the same events to a broker later.
- Reassigning claims between agents, and a second approver above a set amount.
- Malware scanning of uploads (Microsoft Defender for Storage).
- Duplicate-document detection using the stored SHA-256.
- Withdrawing, appealing or reopening a claim.
- Payouts.
- Bahasa Malaysia translation.
- Medical claims.
- Faster cold starts with Java's ahead-of-time class cache.

## API surface

| Method | Route | Who | Purpose |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | Anyone | Sets a fresh `XSRF-TOKEN` cookie; the app calls it on load and after signing in or out |
| POST | `/api/auth/login` | Anyone | Sign in with a form holding `email` and `password`; starts a session, answers 204 |
| POST | `/api/auth/logout` | Signed in | End the session |
| GET | `/api/auth/me` | Signed in | Current user and role |
| GET | `/api/policies` | Customer | The customer's policies with limits per claim type |
| GET | `/api/claims` | Both | Customer: own claims. Agent: one queue tab with `?tab=submitted` (the default), `in-review-by-me`, `waiting-on-customer` or `decided` |
| POST | `/api/claims` | Customer | Submit: multipart with a `claim` JSON part and 1 to 5 `files` parts |
| GET | `/api/claims/{id}` | Both | Claim with documents, timeline and current version |
| POST | `/api/claims/{id}/start-review` | Agent | Assign to me, move to Under review |
| POST | `/api/claims/{id}/request-info` | Assigned agent | Ask the customer for something |
| POST | `/api/claims/{id}/respond` | Owner | Multipart: a `response` JSON part (version, note) and up to 5 `files` parts |
| POST | `/api/claims/{id}/approve` | Assigned agent | Amount and reason |
| POST | `/api/claims/{id}/reject` | Assigned agent | Reason |
| GET | `/api/claims/{id}/documents/{documentId}/link` | Owner or agent | A download link that expires after 15 minutes |
| GET | `/api/notifications` | Signed in | My notifications, newest first, with unread count |
| POST | `/api/notifications/{id}/read`, `/read-all` | Signed in | Mark read |
| GET | `/v3/api-docs` | Anyone | OpenAPI document |
| GET | `/actuator/health/liveness`, `/readiness` | Anyone | Health probes for Azure |

Every request that changes a claim sends the version number it read in its body. Every request that changes anything carries a CSRF token.

## Milestones (part-time, about 10 hours a week)

| Week | Goal | Done when |
| --- | --- | --- |
| 1 | Learn and frame | Spring and React starter tutorials done; Figma user flows and wireframes for both roles; empty repo builds (done 7 Nov); Docker running on the PC (done 7 Nov) |
| 2 | Claims domain and database | State machine with unit tests; JPA entities, Flyway migration, endpoints without security; integration tests on Testcontainers PostgreSQL (done 7 Nov) |
| 3 | Security and documents | Sign-in, roles, ownership and CSRF with tests; upload to Azurite, file checks, download links (done 8 Nov) |
| 4 | Live early | Pipeline green; app running on Container Apps with the database and storage; notifications module working through events (done 10 Nov) |
| 5 | Design system | High-fidelity Figma screens and variables; tokens and shared components in Storybook with accessibility checks passing (components done 14 Nov); the pipeline builds and tests the web app, and the image serves it with the Storybook |
| 6 | Screens | Customer and agent screens wired to the API; the demo story runs on the live link |
| 7 | Usability test | Five sessions done, findings rated, top fixes shipped |
| 8 | Polish and story | README with GIF, architecture diagram, CI badge and case study; reset logic; LinkedIn post |

Keep applying with ToolFleet throughout. If a week runs over, protect week 4 (live link and green CI) and week 7 (the usability test): those two are what make this project different from other applicants' CRUD apps.

## Decisions from week 1

- Docker on the PC: settled 7 November. Docker Desktop 4.98.0 runs on WSL 2 and the first Testcontainers test passed, so integration tests run locally as well as in the pipeline. The no-Docker fallback isn't needed.
- The name: settled 7 November. No insurer or takaful operator called Teduh turned up. The nearest is Touch 'n Go's "Tenang" plans, a different word. Keep the look clearly separate from real insurers' colours and marks, and from Touch 'n Go's blue.
- Container registry: settled 7 November. A public image on GitHub Container Registry (`ghcr.io/hussenoloyede/teduh-claims`), free, so the plan has no fixed monthly cost. The pipeline pushes with a classic GitHub token limited to `write:packages`, with an expiry, which Hussein creates and stores in an Azure DevOps service connection himself. Container Apps pulls the public image with no credentials. Azure Container Registry Basic (about US$5 a month, no token) was the alternative.

## Decisions from week 2

- `ClaimType` lives in the policies module, because a policy decides which types it covers and what each limit is. Claims depends on policies, never the other way.
- Spring Modulith uses its JDBC starter, and Flyway creates its `event_publication` table, so Hibernate's `validate` check only ever looks at our own entities.
- Every "now" comes from one `Clock` bean, and "today" means today in Malaysia (`Asia/Kuala_Lumpur`). Tests fix the clock at 7 Nov 2026, 9:14 pm.
- The agent queue tabs in the API are `submitted`, `in-review-by-me`, `waiting-on-customer` and `decided`, matching the wireframes.
- Until week 3, the acting user comes from an `X-Demo-User-Id` header and all requests are allowed. Both go when real sign-in lands, and the week 3 tests prove it.

## Decisions from week 3

- Sign-in uses Spring Security's own form login at `/api/auth/login` (form fields, 204 on success) rather than a hand-written JSON endpoint, so session fixation protection, CSRF token renewal and saving the session all come from Spring Security.
- The CSRF token lives in a cookie the React app can read, and `GET /api/auth/csrf` hands out a fresh one, because Spring Security clears it at sign-in and sign-out.
- The demo password (`teduh-demo`) is not a secret: the demo buttons will send it, so it will be in the frontend code. The demo accounts only ever see demo data.
- An info request's message is kept on its timeline line (`claim_events.message`), so the planned `info_requests` table isn't needed.
- Files go through the Azure SDK for Java with Java's own `HttpClient` in place of Netty, so its version can't clash with Spring Boot's. Azurite runs in Testcontainers as a plain container, since Spring Boot has no service connection for it.
- Tests get CSRF tokens the way the browser does, never through Spring Security's `csrf()` test helper, which swaps the app's token store for the rest of the test run.

## Decisions from week 4

- One event, `ClaimStatusChanged`, with the from and to status, replaces the six event types first planned. The listener picks the recipient with a `switch` on the new status.
- Delivered events are deleted from `event_publication` (`completion-mode: delete`), and undelivered ones are sent again on restart, which matters because the app scales to zero.
- The app reaches PostgreSQL through Azure's JDBC authentication plugin (`azure-identity-extensions`) and its managed identity, not the Spring Cloud Azure starter. The server accepts Entra sign-ins only.
- The Container Apps environment runs in workload profiles mode with the Consumption profile, created from the CLI, because Express environments don't support a system-assigned managed identity.
- At most one replica, because sessions are held in memory.
- `docs/azure-setup.md` stays out of the public repo (`.git/info/exclude`); a private copy is in the project.
- The Azure DevOps project is private (the organisation allows no public projects), so the README will show a status badge and a screenshot of a green run.

## Decisions from week 5

- The screens are Hussein's "Hi-Fi 2.0" page in Figma: 15 customer screens (C01 to C15, including Claims, Policies and Profile behind the bottom nav) and 12 agent screens (A01 to A12, including Assigned reviews and History behind the header nav). Every page maps to an endpoint that already exists.
- Design tokens follow Hi-Fi 2.0: dark green for the customer app, blue for the agent app. In Figma they are one Color collection with a Customer mode and an Agent mode; in code, `web/src/tokens.css` with `[data-theme="agent"]` overriding the colours. Three greys and the agent green were darkened slightly so text passes 4.5:1.
- Type: Inter for body text (11 to 16 px), Plus Jakarta Sans for headings (18 to 28 px), and an Overline style for the uppercase labels.
- Left out of the MVP even though the screens show them, and listed in the README as what comes next: DuitNow payouts, the emergency hotline, dispute and re-review, PDF downloads, Remember me and Forgot password, and the illustrative KPI figures on the agent queue.
- The React app is generated with the official Vite template (React 19, TypeScript) and Storybook 10 with its docs, test and accessibility add-ons.
- No UI kit: the components are Hussein's own, styled with CSS Modules and the tokens. Icons come from lucide-react. People are shown as initials, never photos.
- Where the build differs from Hi-Fi 2.0 on purpose: documents say Download rather than View (the links download as attachments); the claim card shows the claimed amount only, because the claim summary has no approved amount; the timeline has no time estimates or SLA text; the current bottom tab gets a pale pill and the unread dot a darker rim, because colour alone was too faint; labels are in sentence case.
- Every date and time is shown in Malaysian time (`Asia/Kuala_Lumpur`), whatever the browser's own time zone, through one formatting module with its own unit tests.
- The Docker image builds the React app and the Storybook in a Node stage, and Spring Boot serves them from the jar: the app at `/`, the Storybook at `/storybook/`. Pages may be shown in frames from Teduh itself only (`X-Frame-Options: SAMEORIGIN`), which the Storybook needs.
- The pipeline's Test stage runs a Web job next to the Java job: lint, type check and build, the unit tests and every story in headless Chromium with axe, and the Storybook build. Either failing stops the image and the deploy.
