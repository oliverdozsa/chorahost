package pub.galactic.chorahost.dto;

/**
 * Optional overrides applied on top of the template when instantiating a new
 * {@link pub.galactic.chorahost.entity.ChoreCard}. Any null field falls back
 * to the template's value.
 */
public record InstantiateCardRequest(
        String title,
        String description,
        Integer estimatedMinutes,
        Integer xpReward,
        Integer goldReward,
        String itemDrop) {
}
