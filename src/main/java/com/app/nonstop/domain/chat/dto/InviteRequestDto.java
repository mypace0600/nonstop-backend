package com.app.nonstop.domain.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class InviteRequestDto {
    @NotEmpty(message = "At least one user must be invited")
    private Set<Long> userIds;
}
