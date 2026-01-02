package com.app.nonstop.domain.community.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Community extends BaseTimeEntity {
    private Long id;
    private Long universityId;
    private String name;
    private String description;
    private String icon;
    private Boolean isAnonymous;
    private Integer sortOrder;
}
