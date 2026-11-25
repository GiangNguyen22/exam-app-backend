package com.android.app.exam_app_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TASK-S5: Unauthenticated access to /uploads/** returns 401 Unauthorized")
    void unauthenticatedAccessToUploadsReturns401() throws Exception {
        mockMvc.perform(get("/uploads/any-image.png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TASK-S5: Uploading .jsp or .html or non-image file is rejected with 400 Bad Request")
    void uploadDisallowedFileTypesReturns400() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");

        // Attempt .jsp upload
        MockMultipartFile jspFile = new MockMultipartFile(
                "file",
                "shell.jsp",
                "application/x-jsp",
                "<% out.println(\"hacked\"); %>".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(jspFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.message").value("Chỉ chấp nhận file ảnh định dạng JPEG hoặc PNG hợp lệ."));

        // Attempt .html upload
        MockMultipartFile htmlFile = new MockMultipartFile(
                "file",
                "xss.html",
                "text/html",
                "<script>alert(1)</script>".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(htmlFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TASK-S5: Uploading valid PNG file with proper magic bytes succeeds")
    void uploadValidImageSucceeds() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");

        // Valid PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};

        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "question-diagram.png",
                "image/png",
                validPngBytes);

        mockMvc.perform(multipart("/api/files/upload")
                        .file(pngFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").exists());
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = response.indexOf(marker);
        int from = start + marker.length();
        int to = response.indexOf("\"", from);
        return response.substring(from, to);
    }
}
