package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(@NotBlank String code) {
}
