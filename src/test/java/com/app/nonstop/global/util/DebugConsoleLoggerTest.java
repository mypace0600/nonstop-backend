package com.app.nonstop.global.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class DebugConsoleLoggerTest {

    @Test
    void testLogger() {
        DebugConsoleLogger.log("일반 디버그 로그 테스트");
        DebugConsoleLogger.success("성공 로그 테스트");
        DebugConsoleLogger.warn("경고 로그 테스트");
        DebugConsoleLogger.error("에러 로그 테스트");

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("id", 12345);
        testMap.put("name", "테스트 유저");
        testMap.put("roles", new String[]{"USER", "ADMIN"});
        testMap.put("isActive", true);

        DebugConsoleLogger.dump("테스트 객체 덤프", testMap);
    }
}
