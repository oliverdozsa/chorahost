package pub.galactic.chorahost.dto;

import java.util.UUID;

import pub.galactic.chorahost.enums.TeamRole;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull UUID userId,
        @NotNull TeamRole role) {
}
