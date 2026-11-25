package com.android.app.exam_app_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProctoringEventSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TASK-S7: Proctoring event with standardized SecurityEventType succeeds and is stored correctly")
    void standardizedProctoringEventsArePersisted() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String studentToken = loginAndExtractToken("student1", "student123");

        Long examId = createExamAndGetId(teacherToken);

        // 1. Send standardized APP_BACKGROUND
        mockMvc.perform(post("/api/exams/" + examId + "/proctoring-events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"APP_BACKGROUND\",\"details\":\"App went to background\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("APP_BACKGROUND"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        // 2. Send legacy FOCUS_LOST (should normalize to APP_BACKGROUND)
        mockMvc.perform(post("/api/exams/" + examId + "/proctoring-events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"FOCUS_LOST\",\"details\":\"Focus lost legacy event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("APP_BACKGROUND"));

        // 3. Send SCREENSHOT_ATTEMPT
        mockMvc.perform(post("/api/exams/" + examId + "/proctoring-events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"SCREENSHOT_ATTEMPT\",\"details\":\"Screenshot detected API 34\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("SCREENSHOT_ATTEMPT"));

        // 4. Verify summary returns correct alert label
        mockMvc.perform(get("/api/exams/" + examId + "/proctoring-events/summary")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].hasAlert").value(true));
    }

    private Long createExamAndGetId(String teacherToken) throws Exception {
        MvcResult examResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Proctoring Test Exam\",\"durationMinutes\":60,\"scorePerQuestion\":1}"))
                .andExpect(status().isOk())
                .andReturn();

        String response = examResult.getResponse().getContentAsString();
        String marker = "\"id\":";
        int start = response.indexOf(marker);
        int from = start + marker.length();
        int to = response.indexOf(",", from);
        if (to < 0) {
            to = response.indexOf("}", from);
        }
        return Long.parseLong(response.substring(from, to).trim());
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
