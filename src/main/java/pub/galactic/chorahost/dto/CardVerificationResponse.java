package pub.galactic.chorahost.dto;

public record CardVerificationResponse(
        ChoreCardResponse cardCompleted,
        int xpGained,
        int goldGained,
        boolean leveledUp,
        int newLevel) {
}
