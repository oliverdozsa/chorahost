create table team_invitations (
    id uuid primary key,
    team_id uuid not null references teams(id),
    email varchar(255) not null,
    code_hash varchar(64) not null unique,
    invited_by uuid not null references users(id),
    status varchar(20) not null,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    accepted_at timestamptz
);

create index idx_team_invitations_team_id on team_invitations(team_id);
create index idx_team_invitations_email on team_invitations(email);

-- At most one open invitation per team and email; re-inviting revokes the old one.
create unique index uq_team_invitations_pending
    on team_invitations(team_id, email) where status = 'PENDING';
