-- Claims and their timelines.
-- customer_id and policy_id point at tables that arrive with the accounts and policies modules.
-- They get foreign keys in the migrations that create those tables.

-- Numbers for references such as CLM-2026-000017. A sequence never hands out the same number twice,
-- even when two claims are submitted at the same moment.
create sequence claim_reference_seq start with 1;

create table claims (
    id                uuid          primary key,
    reference         varchar(20)   not null unique,
    customer_id       uuid          not null,
    policy_id         uuid          not null,
    claim_type        varchar(30)   not null,
    incident_date     date          not null,
    amount_claimed    numeric(12,2) not null,
    description       varchar(500)  not null,
    status            varchar(20)   not null,
    assigned_agent_id uuid,
    approved_amount   numeric(12,2),
    decision_reason   varchar(1000),
    submitted_at      timestamptz   not null,
    version           bigint        not null,

    constraint claims_claim_type_check check (claim_type in
        ('FLIGHT_DELAY', 'BAGGAGE_DELAYED', 'LOST_OR_DAMAGED_BAGGAGE', 'TRIP_CANCELLATION')),
    constraint claims_status_check check (status in
        ('SUBMITTED', 'UNDER_REVIEW', 'MORE_INFO_NEEDED', 'APPROVED', 'REJECTED')),
    constraint claims_amount_claimed_check check (amount_claimed > 0),
    constraint claims_approved_amount_check check (approved_amount is null
        or (approved_amount > 0 and approved_amount <= amount_claimed))
);

-- A customer's own claims, and the agent queue (by status, oldest first).
create index claims_customer_idx on claims (customer_id);
create index claims_queue_idx on claims (status, submitted_at);

create table claim_events (
    id          bigint        generated always as identity primary key,
    claim_id    uuid          not null references claims (id),
    from_status varchar(20),
    to_status   varchar(20)   not null,
    actor_id    uuid          not null,
    message     varchar(1000),
    occurred_at timestamptz   not null
);

create index claim_events_claim_idx on claim_events (claim_id, occurred_at, id);
