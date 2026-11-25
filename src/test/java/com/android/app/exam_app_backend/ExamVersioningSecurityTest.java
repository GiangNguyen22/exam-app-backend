package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamVersioningSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    private Long defaultSubjectId;

    @BeforeEach
    void setUp() {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Test Subject");
            s.setDescription("Test Desc");
            return subjectRepository.save(s);
        });
        defaultSubjectId = subject.getId();
    }

    @Test
    @DisplayName("TASK-T2: Exam versioning state machine and modification lock")
    void testExamVersioningWorkflowAndLock() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String adminToken = loginAndExtractToken("admin", "admin123");

        // 1. Tạo đề thi mới -> Khởi tạo Version 1 với status DRAFT
        MvcResult createExamResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"State Machine Test Exam\",\"durationMinutes\":45,\"scorePerQuestion\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.versionNo", is(1)))
                .andReturn();

        Long examId = extractExamId(createExamResult.getResponse().getContentAsString());

        // 2. Thêm câu hỏi khi đang ở DRAFT -> Thành công
        String addQuestionPayload = "{\n" +
                "  \"subjectId\": " + defaultSubjectId + ",\n" +
                "  \"content\": \"What is 2 + 2?\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"difficulty\": \"EASY\",\n" +
                "  \"orderIndex\": 1,\n" +
                "  \"score\": 2.0,\n" +
                "  \"answers\": [\n" +
                "    {\"content\": \"3\", \"correct\": false},\n" +
                "    {\"content\": \"4\", \"correct\": true}\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(post("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addQuestionPayload))
                .andExpect(status().isOk());

        // 3. Nộp duyệt đề thi (submitExamForReview) -> Chuyển sang SUBMITTED
        mockMvc.perform(post("/api/exams/" + examId + "/submit-review")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.versionNo", is(1)));

        // 4. Khi đang SUBMITTED, các thao tác chỉnh sửa đề / câu hỏi / xóa đề BỊ KHÓA (HTTP 409 CONFLICT)
        // a. Sửa thông tin đề
        mockMvc.perform(put("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Modified Title When Submitted\",\"durationMinutes\":60,\"scorePerQuestion\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SUBMITTED")));

        // b. Thêm câu hỏi
        mockMvc.perform(post("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addQuestionPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SUBMITTED")));

        // c. Xóa đề
        mockMvc.perform(delete("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SUBMITTED")));

        // 5. Yêu cầu chỉnh sửa (requestChanges) bởi Admin -> Tạo Version 2 ở DRAFT, Version 1 thành CHANGES_REQUESTED
        mockMvc.perform(post("/api/exams/" + examId + "/request-changes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comments\":\"Please check question 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.versionNo", is(2)));

        // 6. Kiểm tra lịch sử phiên bản đề thi
        mockMvc.perform(get("/api/exams/" + examId + "/versions")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNo", is(2)))
                .andExpect(jsonPath("$.data[0].status", is("DRAFT")))
                .andExpect(jsonPath("$.data[1].versionNo", is(1)))
                .andExpect(jsonPath("$.data[1].status", is("CHANGES_REQUESTED")));

        // 7. Ở Version 2 (DRAFT), giáo viên có thể chỉnh sửa lại đề bình thường
        mockMvc.perform(put("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revised Title for V2\",\"durationMinutes\":50,\"scorePerQuestion\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("Revised Title for V2")));
    }

    private Long extractExamId(String response) {
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
