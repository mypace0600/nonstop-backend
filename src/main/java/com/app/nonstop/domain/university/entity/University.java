package com.app.nonstop.domain.university.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 대학 정보를 나타내는 데이터 객체(POJO).
 * `universities` 테이블의 레코드와 매핑됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class University {
    private Long id;
    private String name;
    private String region;
    private String logoImageUrl;
    private LocalDateTime createdAt;
}
