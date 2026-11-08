-- Records of the files attached to claims. The files themselves are in Blob Storage, under blob_name.

create table documents (
    id           uuid         primary key,
    claim_id     uuid         not null references claims (id),
    file_name    varchar(100) not null,
    -- Worked out from the file's first bytes, never taken from the browser.
    content_type varchar(20)  not null,
    size_bytes   bigint       not null,
    -- SHA-256 as 64 hex characters.
    sha256       varchar(64)  not null,
    blob_name    varchar(300) not null unique,
    uploaded_by  uuid         not null references users (id),
    uploaded_at  timestamptz  not null,

    constraint documents_content_type_check check (content_type in ('application/pdf', 'image/jpeg', 'image/png')),
    constraint documents_size_check check (size_bytes > 0 and size_bytes <= 10485760)
);

create index documents_claim_idx on documents (claim_id, uploaded_at);
