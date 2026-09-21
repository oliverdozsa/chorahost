alter table chore_card_templates
    add column recurrence_type text,
    add column recurrence_interval_minutes integer;

alter table chore_cards
    add column recurrence_type text,
    add column recurrence_interval_minutes integer,
    add column recurrence_next_spawn_at timestamptz;

create index idx_chore_cards_recurrence_next_spawn_at
    on chore_cards(recurrence_next_spawn_at)
    where recurrence_next_spawn_at is not null;
