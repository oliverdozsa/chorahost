-- Gamification stats (level/XP/gold) move from the user (global) to the
-- team membership (per-team), since a user now belongs to many teams and
-- should progress independently in each.
alter table team_memberships
    add column level integer not null default 1,
    add column current_xp integer not null default 0,
    add column gold integer not null default 0;

alter table users
    drop column level,
    drop column current_xp,
    drop column gold;
