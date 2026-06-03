create table app_users (
    id bigserial primary key,
    username varchar(100) not null unique,
    password_hash varchar(255) not null,
    created_at timestamptz not null default now()
);

create table notes (
    id bigserial primary key,
    owner_id bigint not null references app_users(id) on delete cascade,
    content text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table note_shares (
    id bigserial primary key,
    note_id bigint not null references notes(id) on delete cascade,
    shared_with_user_id bigint not null references app_users(id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint uq_note_shares_note_user unique (note_id, shared_with_user_id)
);

create index idx_notes_owner_id on notes(owner_id);
create index idx_note_shares_note_id on note_shares(note_id);
create index idx_note_shares_shared_with_user_id on note_shares(shared_with_user_id);
