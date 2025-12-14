package com.app.nonstop.global.common.entity;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모든 엔티티에서 공통으로 사용하는 생성일시, 수정일시를 관리하기 위한 부모 클래스.
 * - 코드 중복을 제거하고,
 * - 생성/수정일에 대한 공통적인 로직 관리를 용이하게 합니다.
 *
 * MyBatis 환경에서는 DB 트리거나 쿼리 작성 시 이 필드들의 값을 직접 관리해주어야 합니다.
 * - INSERT 시: created_at, updated_at 에 현재 시간 삽입
 * - UPDATE 시: updated_at 에만 현재 시간 삽입
 */
@Getter
public abstract class BaseTimeEntity {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
