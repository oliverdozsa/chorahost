package pub.galactic.chorahost.dto;

import java.util.UUID;

/** A member's final standing in an ended season. */
public record SeasonResultResponse(
        UUID userId,
        String name,
        int rank,
        int level,
        int currentXp,
        int gold,
        int cardsVerified) {
}
