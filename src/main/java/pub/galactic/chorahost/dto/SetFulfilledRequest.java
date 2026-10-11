package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.NotNull;

public record SetFulfilledRequest(@NotNull Boolean fulfilled) {
}
