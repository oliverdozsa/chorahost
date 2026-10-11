-- Per-team shop: admins list items, members spend their gold on them.
-- stock is null for unlimited items; items are soft-deleted (active=false)
-- so purchase history keeps pointing at them.
create table shop_items (
    id uuid primary key,
    team_id uuid not null references teams(id),
    name varchar(255) not null,
    price integer not null check (price >= 0),
    stock integer check (stock >= 0),
    image_url varchar(2048),
    active boolean not null default true,
    created_at timestamptz not null
);
create index idx_shop_items_team on shop_items(team_id);

create table shop_purchases (
    id uuid primary key,
    team_id uuid not null references teams(id),
    user_id uuid not null references users(id),
    item_id uuid not null references shop_items(id),
    item_name varchar(255) not null,
    unit_price integer not null,
    quantity integer not null check (quantity >= 1),
    total_price bigint not null,
    fulfilled boolean not null default false,
    fulfilled_at timestamptz,
    created_at timestamptz not null
);
create index idx_shop_purchases_team on shop_purchases(team_id, created_at desc);
create index idx_shop_purchases_user on shop_purchases(team_id, user_id, created_at desc);
