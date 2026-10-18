-- Per-user image overrides for a card template; template image_url stays the fallback.
create table chore_card_template_user_images (
    template_id uuid not null references chore_card_templates(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    image_url text not null,
    primary key (template_id, user_id)
);
