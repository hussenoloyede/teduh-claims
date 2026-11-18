-- Sign-in sessions, kept in the database by Spring Session JDBC so they outlive the app process.
-- Container Apps stops the app after a few idle minutes; with sessions in memory everyone was signed out
-- and lost anything half-typed. This is Spring Session's own PostgreSQL schema (schema-postgresql.sql),
-- created here so Flyway owns every table, as with event_publication in V2.

create table spring_session (
    primary_id            char(36)     not null,
    session_id            char(36)     not null,
    creation_time         bigint       not null,
    last_access_time      bigint       not null,
    max_inactive_interval int          not null,
    expiry_time           bigint       not null,
    -- The signed-in person's username (their email), so their sessions can be found.
    principal_name        varchar(100),
    constraint spring_session_pk primary key (primary_id)
);

create unique index spring_session_ix1 on spring_session (session_id);
create index spring_session_ix2 on spring_session (expiry_time);
create index spring_session_ix3 on spring_session (principal_name);

-- What the session holds, such as who is signed in, as Java-serialized bytes.
create table spring_session_attributes (
    session_primary_id char(36)     not null,
    attribute_name     varchar(200) not null,
    attribute_bytes    bytea        not null,
    constraint spring_session_attributes_pk primary key (session_primary_id, attribute_name),
    constraint spring_session_attributes_fk foreign key (session_primary_id)
        references spring_session (primary_id) on delete cascade
);
