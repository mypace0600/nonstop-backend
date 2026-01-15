package com.app.nonstop.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class GlobalRequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(requestWrapper, responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();

        String fullUri = queryString == null ? uri : uri + "?" + queryString;

        log.info("HTTP {} {} | Status: {} | Duration: {}ms", method, fullUri, status, duration);

        byte[] requestBody = request.getContentAsByteArray();
        byte[] responseBody = response.getContentAsByteArray();

        if (requestBody.length > 0) {
            log.debug("Request Body: {}", getBodyString(requestBody, request.getCharacterEncoding()));
        }
        if (responseBody.length > 0) {
            log.debug("Response Body: {}", getBodyString(responseBody, response.getCharacterEncoding()));
        }
    }

    private String getBodyString(byte[] body, String encoding) {
        try {
            return new String(body, encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "[Binary Content]";
        }
    }
}
