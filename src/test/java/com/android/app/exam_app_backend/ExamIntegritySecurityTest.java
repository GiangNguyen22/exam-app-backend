package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamVersion;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.security.crypto.CryptoKeyProvider;
import com.android.app.exam_app_backend.service.AuditLogService;
import com.android.app.exam_app_backend.service.ExamIntegrityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamIntegritySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ExamIntegrityService examIntegrityService;

    @Autowired
    private CryptoKeyProvider cryptoKeyProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long defaultSubjectId;

    @BeforeEach
    void setUp() {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Integrity Subject");
            s.setDescription("Subject for HMAC tests");
            return subjectRepository.save(s);
        });
        defaultSubjectId = subject.getId();
    }

    @Test
    @DisplayName("TASK-T4: HMAC-SHA256 key is strictly separated from AES encryption key")
    void testIntegrityKeySeparatedFromEncryptionKey() {
        assertNotNull(cryptoKeyProvider.getExamSecretKey());
        assertNotNull(cryptoKeyProvider.getIntegritySecretKey());
        assertFalse(Arrays.equals(
                cryptoKeyProvider.getExamSecretKey().getEncoded(),
                cryptoKeyProvider.getIntegritySecretKey().getEncoded()
        ), "HMAC-SHA256 integrity key must be separate from AES encryption key");
    }

    @Test
    @DisplayName("Test B2: Tampering 1 character of question content in DB triggers ExamIntegrityCompromisedException (409) + INTEGRITY_MISMATCH audit log")
    void testTamperedQuestionContentInDbIsDetected() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String approverToken = loginAndExtractToken("approver1", "approver123");

        Long examId = createApprovedExam(teacherToken, approverToken, "Integrity Protected Exam");

        // 1. Verify integrity passes before tampering
        mockMvc.perform(post("/api/exams/" + examId + "/verify-integrity")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.integrityTag", notNullValue()));

        // 2. Directly tamper 1 character / content of question in DB bypassing JPA & service layer
        Long questionId = jdbcTemplate.queryForObject(
                "SELECT question_id FROM exam_questions WHERE exam_id = ? ORDER BY order_index ASC LIMIT 1",
                Long.class,
                examId
        );
        assertNotNull(questionId);

        jdbcTemplate.update(
                "UPDATE questions SET content = ? WHERE id = ?",
                "What is 2 + 3?",
                questionId
        );

        // 3. Attempting to verify integrity or fetch questions is now blocked (409 Conflict)
        mockMvc.perform(post("/api/exams/" + examId + "/verify-integrity")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("HMAC-SHA256")));

        mockMvc.perform(get("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isConflict());

        // 4. Verify INTEGRITY_MISMATCH security alert was logged in audit_logs
        Long versionId = jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM exams WHERE id = ?",
                Long.class,
                examId
        );
        List<AuditLog> mismatchLogs = auditLogRepository.searchLogs(
                AuditLogService.ACTION_INTEGRITY_MISMATCH, "exam_version", versionId);
        assertFalse(mismatchLogs.isEmpty(), "INTEGRITY_MISMATCH must be recorded in audit_logs");
        assertEquals(AuditResult.deny, mismatchLogs.get(0).getResult());
    }

    @Test
    @DisplayName("Test B2: Tampering integrity_tag directly in DB is detected")
    void testTamperedIntegrityTagInDbIsDetected() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String approverToken = loginAndExtractToken("approver1", "approver123");

        Long examId = createApprovedExam(teacherToken, approverToken, "Integrity Tag Tamper Exam");

        Long versionId = jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM exams WHERE id = ?",
                Long.class,
                examId
        );
        String originalTag = jdbcTemplate.queryForObject(
                "SELECT integrity_tag FROM exam_versions WHERE id = ?",
                String.class,
                versionId
        );
        assertNotNull(originalTag);
        assertEquals(64, originalTag.length());

        String flippedChar = originalTag.startsWith("a") ? "b" : "a";
        String tamperedTag = flippedChar + originalTag.substring(1);
        assertNotEquals(originalTag, tamperedTag);

        jdbcTemplate.update("UPDATE exam_versions SET integrity_tag = ? WHERE id = ?", tamperedTag, versionId);

        mockMvc.perform(get("/api/exams/" + examId + "/integrity")
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isConflict());

        List<AuditLog> mismatchLogs = auditLogRepository.searchLogs(
                AuditLogService.ACTION_INTEGRITY_MISMATCH, "exam_version", versionId);
        assertFalse(mismatchLogs.isEmpty());
    }

    @Test
    @DisplayName("TASK-T4: Canonical JSON sorts ExamQuestion by orderIndex and Answer by id deterministically")
    void testCanonicalJsonDeterministicSorting() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String approverToken = loginAndExtractToken("approver1", "approver123");

        // Create exam with questions added in reverse orderIndex (2 then 1)
        MvcResult createExamResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Canonical Order Exam\",\"durationMinutes\":30,\"scorePerQuestion\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        Long examId = extractExamId(createExamResult.getResponse().getContentAsString());

        String q2Payload = "{\n" +
                "  \"subjectId\": " + defaultSubjectId + ",\n" +
                "  \"content\": \"Second question by orderIndex\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"difficulty\": \"EASY\",\n" +
                "  \"orderIndex\": 2,\n" +
                "  \"score\": 1.0,\n" +
                "  \"answers\": [{\"content\": \"B1\", \"correct\": true}, {\"content\": \"B2\", \"correct\": false}]\n" +
                "}";
        String q1Payload = "{\n" +
                "  \"subjectId\": " + defaultSubjectId + ",\n" +
                "  \"content\": \"First question by orderIndex\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"difficulty\": \"EASY\",\n" +
                "  \"orderIndex\": 1,\n" +
                "  \"score\": 1.0,\n" +
                "  \"answers\": [{\"content\": \"A1\", \"correct\": true}, {\"content\": \"A2\", \"correct\": false}]\n" +
                "}";

        mockMvc.perform(post("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(q2Payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(q1Payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exams/" + examId + "/submit-review")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/approve")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved for canonical order test\"}"))
                .andExpect(status().isOk());

        String canonical1 = transactionTemplate.execute(status -> {
            Exam exam = examRepository.findById(examId).orElseThrow();
            ExamVersion version = exam.getCurrentVersion();
            return examIntegrityService.buildCanonicalJson(exam, version);
        });
        String canonical2 = transactionTemplate.execute(status -> {
            Exam exam = examRepository.findById(examId).orElseThrow();
            ExamVersion version = exam.getCurrentVersion();
            return examIntegrityService.buildCanonicalJson(exam, version);
        });
        assertNotNull(canonical1);
        assertEquals(canonical1, canonical2);

        int idxQ1 = canonical1.indexOf("First question by orderIndex");
        int idxQ2 = canonical1.indexOf("Second question by orderIndex");
        assertTrue(idxQ1 >= 0 && idxQ2 > idxQ1, "Questions must be ordered by orderIndex ascending in canonical JSON");
    }

    private Long createApprovedExam(String teacherToken, String approverToken, String title) throws Exception {
        MvcResult createExamResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"durationMinutes\":45,\"scorePerQuestion\":2}"))
                .andExpect(status().isOk())
                .andReturn();

        Long examId = extractExamId(createExamResult.getResponse().getContentAsString());

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

        mockMvc.perform(post("/api/exams/" + examId + "/submit-review")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/approvals/exams/" + examId + "/approve")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved for integrity verification\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examStatus", is("APPROVED")));

        return examId;
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
