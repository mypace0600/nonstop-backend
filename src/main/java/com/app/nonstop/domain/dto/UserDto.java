package com.app.nonstop.domain.dto;

import com.app.nonstop.domain.user.UserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String email;
    private UserType type;
    private boolean isAdmin;
}
