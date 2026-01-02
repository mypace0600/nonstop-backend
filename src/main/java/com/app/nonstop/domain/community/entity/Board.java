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
public class Board extends BaseTimeEntity {
    private Long id;
    private Long communityId;
    private String name;
    private BoardType type;
    private Boolean isSecret;
}
