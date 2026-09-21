-- SCHEDULE recurrence moves from a fixed minute interval to calendar rules.
-- Any SCHEDULE rows created under the old interval model cannot be converted
-- faithfully, so they stop recurring.
update chore_cards
set recurrence_type = null, recurrence_interval_minutes = null, recurrence_next_spawn_at = null
where recurrence_type = 'SCHEDULE';

update chore_card_templates
set recurrence_type = null, recurrence_interval_minutes = null
where recurrence_type = 'SCHEDULE';

alter table chore_card_templates
    add column schedule_frequency text,
    add column schedule_interval integer,
    add column schedule_days_of_week text,
    add column schedule_day_of_month integer,
    add column schedule_time time,
    add column schedule_time_zone text;

alter table chore_cards
    add column schedule_frequency text,
    add column schedule_interval integer,
    add column schedule_days_of_week text,
    add column schedule_day_of_month integer,
    add column schedule_time time,
    add column schedule_time_zone text;
