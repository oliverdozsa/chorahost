package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(@NotBlank String name) {
}
