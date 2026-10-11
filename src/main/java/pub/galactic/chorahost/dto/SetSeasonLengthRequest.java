package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetSeasonLengthRequest(
        @NotNull @Min(7) @Max(365) Integer days) {
}
