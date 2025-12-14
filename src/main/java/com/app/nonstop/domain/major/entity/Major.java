package com.app.nonstop.domain.major.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전공 정보를 나타내는 데이터 객체(POJO).
 * `majors` 테이블의 레코드와 매핑됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Major {
    private Long id;
    private Long universityId;
    private String name;
}
