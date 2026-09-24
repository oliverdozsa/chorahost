alter table chore_card_templates
    add column due_date_interval_hours integer;

alter table chore_cards
    add column due_date_interval_hours integer,
    add column due_at timestamptz;
