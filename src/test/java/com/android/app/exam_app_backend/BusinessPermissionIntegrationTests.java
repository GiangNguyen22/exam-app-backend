package com.android.app.exam_app_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessPermissionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teacherCanCreateQuestion() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":1,\"topicId\":1,\"content\":\"What is Java?\",\"type\":\"SINGLE\",\"difficulty\":\"EASY\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotCreateQuestion() throws Exception {
        String studentToken = loginAndExtractToken("student1", "student123");

        mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":1,\"topicId\":1,\"content\":\"Blocked\",\"type\":\"SINGLE\",\"difficulty\":\"EASY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanSubmitExam() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String studentToken = loginAndExtractToken("student1", "student123");
        Long examId = createExamAndGetId(teacherToken);

        mockMvc.perform(post("/api/exams/" + examId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"done\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotGenerateExam() throws Exception {
        String studentToken = loginAndExtractToken("student1", "student123");

        mockMvc.perform(post("/api/exams/generate")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Auto\", \"durationMinutes\":30, \"scorePerQuestion\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanViewResults() throws Exception {
        String adminToken = loginAndExtractToken("admin", "admin123");
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String studentToken = loginAndExtractToken("student1", "student123");
        Long examId = createExamAndGetId(adminToken);

        mockMvc.perform(post("/api/exams/" + examId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"done\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/results/" + examId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }

    private Long createExamAndGetId(String teacherToken) throws Exception {
        MvcResult examResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Midterm\",\"durationMinutes\":60,\"scorePerQuestion\":1}"))
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
