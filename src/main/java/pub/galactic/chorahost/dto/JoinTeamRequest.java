package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(@NotBlank @Email String email) {
}
