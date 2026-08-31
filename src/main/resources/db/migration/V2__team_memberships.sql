create table team_memberships (
    id uuid primary key,
    user_id uuid not null references users(id),
    team_id uuid not null references teams(id),
    role varchar(20) not null,
    joined_at timestamptz not null,
    constraint uq_team_memberships_user_team unique (user_id, team_id)
);

create index idx_team_memberships_user_id on team_memberships(user_id);
create index idx_team_memberships_team_id on team_memberships(team_id);

-- Carry forward each user's single existing team/role into the new
-- many-to-many membership table before dropping the old columns.
insert into team_memberships (id, user_id, team_id, role, joined_at)
select gen_random_uuid(), id, team_id, team_role, created_at
from users
where team_id is not null and team_role is not null;

alter table users drop column team_id;
alter table users drop column team_role;
