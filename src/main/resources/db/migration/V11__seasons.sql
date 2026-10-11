-- Seasons: each team runs back-to-back seasons; at the end of one, member
-- level/XP/gold are archived into season_results and reset.
alter table teams add column season_length_days integer not null default 30;

create table seasons (
    id uuid primary key,
    team_id uuid not null references teams(id),
    number integer not null,
    status varchar(20) not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    unique (team_id, number)
);

-- At most one running season per team.
create unique index uq_seasons_active on seasons(team_id) where status = 'ACTIVE';
create index idx_seasons_active_ends_at on seasons(ends_at) where status = 'ACTIVE';

create table season_results (
    id uuid primary key,
    season_id uuid not null references seasons(id),
    user_id uuid not null references users(id),
    rank integer not null,
    level integer not null,
    current_xp integer not null,
    gold integer not null,
    cards_verified integer not null,
    unique (season_id, user_id)
);

-- Existing teams get a first season starting now.
insert into seasons (id, team_id, number, status, starts_at, ends_at)
select gen_random_uuid(), id, 1, 'ACTIVE', now(), now() + interval '30 days' from teams;
