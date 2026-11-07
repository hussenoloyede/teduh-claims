-- Travel policies and how much each one covers per claim type.
-- The foreign keys from claims.policy_id and policies.customer_id arrive with the demo data and
-- accounts in week 3, once there are rows for them to point at.

create table policies (
    id            uuid          primary key,
    policy_number varchar(20)   not null unique,
    customer_id   uuid          not null,
    plan_name     varchar(100)  not null,
    cover_start   date          not null,
    cover_end     date          not null,

    constraint policies_cover_check check (cover_end >= cover_start)
);

create index policies_customer_idx on policies (customer_id);

create table policy_limits (
    policy_id    uuid          not null references policies (id),
    claim_type   varchar(30)   not null,
    limit_amount numeric(12,2) not null,

    primary key (policy_id, claim_type),
    constraint policy_limits_claim_type_check check (claim_type in
        ('FLIGHT_DELAY', 'BAGGAGE_DELAYED', 'LOST_OR_DAMAGED_BAGGAGE', 'TRIP_CANCELLATION')),
    constraint policy_limits_amount_check check (limit_amount > 0)
);
