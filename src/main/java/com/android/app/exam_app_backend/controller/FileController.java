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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_CREATE)")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .header("X-Content-Type-Options", "nosniff")
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("File is empty")
                            .build());
        }

        if (!isValidImage(file)) {
            return ResponseEntity.badRequest()
                    .header("X-Content-Type-Options", "nosniff")
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("Chỉ chấp nhận file ảnh định dạng JPEG hoặc PNG hợp lệ.")
                            .build());
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        String fileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest()
                        .header("X-Content-Type-Options", "nosniff")
                        .body(ApiResponse.<FileUploadResponse>builder()
                                .success(false)
                                .code(HttpStatus.BAD_REQUEST.value())
                                .message("Tên file không hợp lệ.")
                                .build());
            }
            Files.copy(file.getInputStream(), filePath);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String url = baseUrl + "/uploads/" + fileName;
            return ResponseEntity.ok()
                    .header("X-Content-Type-Options", "nosniff")
                    .body(ApiResponse.<FileUploadResponse>builder()
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
                    .header("X-Content-Type-Options", "nosniff")
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Failed to upload file: " + e.getMessage())
                            .build());
        }
    }

    private boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") && !contentType.equalsIgnoreCase("image/png"))) {
            return false;
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return false;
        }
        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!extension.equals(".jpg") && !extension.equals(".jpeg") && !extension.equals(".png")) {
            return false;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                return false;
            }
            // PNG magic bytes: 89 50 4E 47
            boolean isPng = header[0] == (byte) 0x89 &&
                    header[1] == (byte) 0x50 &&
                    header[2] == (byte) 0x4E &&
                    header[3] == (byte) 0x47;

            // JPEG magic bytes: FF D8 FF
            boolean isJpeg = (header[0] & 0xFF) == 0xFF &&
                    (header[1] & 0xFF) == 0xD8 &&
                    (header[2] & 0xFF) == 0xFF;

            if (extension.equals(".png") && isPng) {
                return true;
            }
            if ((extension.equals(".jpg") || extension.equals(".jpeg")) && isJpeg) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
