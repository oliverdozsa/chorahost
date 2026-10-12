-- Long-lived app sessions, minted from a verified Google ID token so clients
-- (notably the installed PWA) don't have to re-run Google sign-in every hour.
-- Only the SHA-256 hash of the session token is stored.
create table auth_sessions (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash varchar(64) not null unique,
    created_at timestamptz not null,
    last_used_at timestamptz not null,
    expires_at timestamptz not null
);
create index idx_auth_sessions_user on auth_sessions(user_id);
create index idx_auth_sessions_expires on auth_sessions(expires_at);
