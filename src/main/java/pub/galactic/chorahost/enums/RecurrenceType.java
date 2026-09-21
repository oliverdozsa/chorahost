package pub.galactic.chorahost.enums;

public enum RecurrenceType {
    /** Respawns on a fixed cadence (every N minutes), anchored to when the card was created. */
    SCHEDULE,
    /** Respawns N minutes after the card was last verified. */
    AFTER_COMPLETION
}
