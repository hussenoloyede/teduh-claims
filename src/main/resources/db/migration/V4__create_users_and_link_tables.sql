-- People who can sign in, and the foreign keys V1 and V3 left out until this table existed.

create table users (
    id            uuid         primary key,
    email         varchar(254) not null unique,
    -- A BCrypt hash with its {bcrypt} prefix, about 68 characters. Never the password itself.
    password_hash varchar(100) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null,

    constraint users_role_check check (role in ('CUSTOMER', 'AGENT')),
    -- The app lower-cases emails before saving; this makes sure nothing else gets around that.
    constraint users_email_lowercase_check check (email = lower(email))
);

-- Every id that names a person or a policy must point at one that exists.
-- Whether it is the right kind of person (a customer, not an agent) is checked by the app.
alter table policies     add constraint policies_customer_fk     foreign key (customer_id)       references users (id);
alter table claims       add constraint claims_customer_fk       foreign key (customer_id)       references users (id);
alter table claims       add constraint claims_policy_fk         foreign key (policy_id)         references policies (id);
alter table claims       add constraint claims_assigned_agent_fk foreign key (assigned_agent_id) references users (id);
alter table claim_events add constraint claim_events_actor_fk    foreign key (actor_id)          references users (id);
