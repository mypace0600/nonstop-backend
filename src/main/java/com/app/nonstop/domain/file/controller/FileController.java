package com.app.nonstop.domain.file.controller;

import com.app.nonstop.domain.file.dto.FileUploadCompleteDto;
import com.app.nonstop.domain.file.dto.FileUploadRequestDto;
import com.app.nonstop.domain.file.service.FileService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File", description = "파일 업로드 관련 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 직접 업로드를 위한 SAS URL 요청", description = "클라이언트가 Azure Blob Storage에 직접 파일을 업로드할 수 있도록 SAS URL을 발급합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/sas-url")
    public ResponseEntity<ApiResponse<String>> requestSasUrl(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody FileUploadRequestDto requestDto
    ) {
        String sasUrl = fileService.generateSasUrl(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success(sasUrl));
    }

    @Operation(summary = "파일 업로드 완료 콜백", description = "클라이언트가 Azure Blob Storage로 파일 업로드를 완료한 후 호출하여 서버에 알립니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/upload-complete")
    public ResponseEntity<ApiResponse<?>> uploadComplete(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody FileUploadCompleteDto completeDto
    ) {
        fileService.processUploadComplete(customUserDetails.getUserId(), completeDto);
        return ResponseEntity.ok(ApiResponse.success());
    }
}