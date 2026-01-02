package com.app.nonstop.global.common.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 공통 생성/수정 시간 엔티티
 *
 * MyBatis 사용 시
 * - INSERT: created_at, updated_at 직접 세팅
 * - UPDATE: updated_at만 세팅 필요
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseTimeEntity {

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
}
