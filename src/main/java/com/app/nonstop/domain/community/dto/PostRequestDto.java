package com.app.nonstop.domain.community.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequestDto {
    private String title;
    private String content;
    private Boolean isAnonymous;
    private Boolean isSecret;
}
