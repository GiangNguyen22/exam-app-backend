package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.FileUploadResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_CREATE)")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("File is empty")
                            .build());
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String url = baseUrl + "/uploads/" + fileName;
            return ResponseEntity.ok(ApiResponse.<FileUploadResponse>builder()
                    .success(true)
                    .code(HttpStatus.OK.value())
                    .message("File uploaded")
                    .data(FileUploadResponse.builder()
                            .fileName(fileName)
                            .url(url)
                            .build())
                    .build());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Failed to upload file: " + e.getMessage())
                            .build());
        }
    }
}
