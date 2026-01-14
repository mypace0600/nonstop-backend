package com.app.nonstop.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * 개발 중 디버깅을 위해 콘솔에 구조화된 로그를 출력하는 유틸리티입니다.
 * <p>
 * 사용법:
 * <pre>
 *     DebugConsoleLogger.log("메시지 확인");
 *     DebugConsoleLogger.dump("User Object", userDto);
 * </pre>
 */
@Slf4j
public class DebugConsoleLogger {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    // ANSI Escape Codes for Colors
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    /**
     * 간단한 디버그 메시지를 출력합니다 (Cyan 색상).
     * @param message 출력할 메시지
     */
    public static void log(String message) {
        System.out.println(ANSI_CYAN + "[DEBUG] " + message + ANSI_RESET);
    }

    /**
     * 강조하고 싶은 디버그 메시지를 출력합니다 (Green 색상).
     * @param message 출력할 메시지
     */
    public static void success(String message) {
        System.out.println(ANSI_GREEN + "[SUCCESS] " + message + ANSI_RESET);
    }

    /**
     * 경고 메시지를 출력합니다 (Purple 색상).
     * @param message 출력할 메시지
     */
    public static void warn(String message) {
        System.out.println(ANSI_PURPLE + "[WARN] " + message + ANSI_RESET);
    }

    /**
     * 에러 메시지를 출력합니다 (Red 색상).
     * @param message 출력할 메시지
     */
    public static void error(String message) {
        System.out.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
    }

    /**
     * 객체의 내용을 JSON 포맷으로 들여쓰기하여 출력합니다. (Yellow 내용, Blue 테두리)
     * @param label 로그 라벨 (식별자)
     * @param object 출력할 객체
     */
    public static void dump(String label, Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            System.out.println(ANSI_BLUE + "================================ [ " + label + " ] ================================" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + json + ANSI_RESET);
            System.out.println(ANSI_BLUE + "================================================================================" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "[DEBUG-FAIL] 객체 덤프 실패 (" + label + "): " + e.getMessage() + ANSI_RESET);
        }
    }
}
