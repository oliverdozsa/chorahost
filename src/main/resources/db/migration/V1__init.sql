create table teams (
    id uuid primary key,
    name varchar(255) not null,
    created_at timestamptz not null
);

create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    name varchar(255) not null,
    level integer not null default 1,
    current_xp integer not null default 0,
    gold integer not null default 0,
    team_id uuid references teams(id),
    team_role varchar(20),
    created_at timestamptz not null
);

create index idx_users_team_id on users(team_id);

create table chore_card_templates (
    id uuid primary key,
    team_id uuid not null references teams(id),
    title varchar(255) not null,
    description text,
    rarity varchar(20) not null,
    complexity integer not null check (complexity between 1 and 10),
    estimated_minutes integer not null,
    xp_reward integer not null,
    gold_reward integer not null,
    item_drop varchar(255),
    created_at timestamptz not null
);

create index idx_chore_card_templates_team_id on chore_card_templates(team_id);

create table chore_cards (
    id uuid primary key,
    title varchar(255) not null,
    description text,
    rarity varchar(20) not null,
    complexity integer not null check (complexity between 1 and 10),
    estimated_minutes integer not null,
    xp_reward integer not null,
    gold_reward integer not null,
    item_drop varchar(255),
    status varchar(20) not null,
    assigned_user_id uuid references users(id),
    team_id uuid not null references teams(id),
    template_id uuid references chore_card_templates(id),
    created_at timestamptz not null,
    claimed_at timestamptz,
    completed_at timestamptz,
    verified_at timestamptz
);

create index idx_chore_cards_team_id on chore_cards(team_id);
create index idx_chore_cards_assigned_user_id on chore_cards(assigned_user_id);
create index idx_chore_cards_status on chore_cards(status);
