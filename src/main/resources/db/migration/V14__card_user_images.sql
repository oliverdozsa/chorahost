-- Per-user image overrides for a card. The card's own image_url remains the
-- fallback for users without an entry here.
create table chore_card_user_images (
    card_id uuid not null references chore_cards(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    image_url text not null,
    primary key (card_id, user_id)
);
