-- In-app notifications: one row per message per person.

create table notifications (
    id           uuid         primary key,
    recipient_id uuid         not null references users (id),
    claim_id     uuid         not null references claims (id),
    message      varchar(300) not null,
    created_at   timestamptz  not null,
    -- Null until the person reads it.
    read_at      timestamptz
);

-- The bell asks for one person's newest notifications, and counts their unread ones.
create index notifications_recipient_idx on notifications (recipient_id, created_at desc);
