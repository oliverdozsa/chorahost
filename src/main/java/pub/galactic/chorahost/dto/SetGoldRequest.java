package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetGoldRequest(
        @NotNull @Min(0) Integer gold) {
}
