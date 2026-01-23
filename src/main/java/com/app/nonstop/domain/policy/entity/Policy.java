package com.app.nonstop.domain.policy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    private Long id;
    private PolicyType type;
    private String title;
    private String content;
    private String url;
    private Boolean isMandatory;
    private String version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
