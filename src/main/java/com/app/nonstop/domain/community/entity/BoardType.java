package com.app.nonstop.domain.community.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {
    GENERAL("GENERAL"),
    NOTICE("NOTICE"),
    QNA("QNA"),
    ANONYMOUS("ANONYMOUS");

    private final String value;
}
