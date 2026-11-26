package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.ExamApproval;
import com.android.app.exam_app_backend.entity.ExamVersion;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.ApprovalDecision;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import com.android.app.exam_app_backend.repository.ExamApprovalRepository;
import com.android.app.exam_app_backend.repository.ExamVersionRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.service.AuditLogService;
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

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamMakerCheckerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ExamApprovalRepository examApprovalRepository;

    @Autowired
    private ExamVersionRepository examVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long defaultSubjectId;

    @BeforeEach
    void setUp() {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Maker Checker Subject");
            s.setDescription("Subject for approval tests");
            return subjectRepository.save(s);
        });
        defaultSubjectId = subject.getId();
    }

    @Test
    @DisplayName("Test B1: Teacher cannot approve own exam (403 Forbidden + AuditLog)")
    void testTeacherCannotApproveOwnExam() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        Long examId = createSubmittedExam(teacherToken, "Teacher Self Approve Exam");

        // Teacher lacks EXAM_APPROVE permission -> 403 Forbidden + permission deny audit log
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Self approve attempt\"}"))
                .andExpect(status().isForbidden());

        List<AuditLog> denyLogs = auditLogRepository.findAllWithUserOrderByCreatedAtDesc();
        assertTrue(denyLogs.stream().anyMatch(log ->
                        log.getResult() == AuditResult.deny
                                && log.getUser() != null
                                && "teacher1".equals(log.getUser().getUsername())),
                "Expected deny audit log when teacher attempted self-approval");
    }

    @Test
    @DisplayName("Test B1: Admin (with EXAM_APPROVE) cannot approve/reject own exam due to Maker-Checker rule")
    void testAdminCannotApproveOwnExamMakerCheckerRule() throws Exception {
        String adminToken = loginAndExtractToken("admin", "admin123");
        Long examId = createSubmittedExam(adminToken, "Admin Maker Checker Exam");

        mockMvc.perform(post("/api/approvals/exams/" + examId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Admin trying to self-approve\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Người tạo đề không được quyền phê duyệt")));

        mockMvc.perform(post("/api/approvals/exams/" + examId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Admin trying to self-reject with enough chars\"}"))
                .andExpect(status().isForbidden());

        List<AuditLog> violations = auditLogRepository.searchLogs(AuditLogService.ACTION_MAKER_CHECKER_VIOLATION, "exam", examId);
        assertFalse(violations.isEmpty(), "Maker-Checker violation must be persisted in audit_logs");
        assertEquals(AuditResult.deny, violations.get(0).getResult());
    }

    @Test
    @DisplayName("TASK-T1 & T3: Approver workflow (pending -> start-review -> approve) signs integrity tag and logs audit")
    void testApproverFullApprovalWorkflow() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String approverToken = loginAndExtractToken("approver1", "approver123");

        Long examId = createSubmittedExam(teacherToken, "Pending Review Exam");

        // 1. Approver views pending list
        mockMvc.perform(get("/api/approvals/pending")
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(examId.intValue())));

        // 2. Approver views exam details for review
        mockMvc.perform(get("/api/approvals/exams/" + examId)
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.id", is(examId.intValue())))
                .andExpect(jsonPath("$.data.questions", hasSize(1)))
                .andExpect(jsonPath("$.data.integritySigned", is(false)));

        // 3. Approver starts review (SUBMITTED -> IN_REVIEW)
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/start-review")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Reviewing exam questions\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision", is("IN_REVIEW")))
                .andExpect(jsonPath("$.data.examStatus", is("IN_REVIEW")));

        // 4. Approver approves exam (IN_REVIEW -> APPROVED) and generates HMAC integrityTag
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/approve")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Exam content verified and approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision", is("APPROVED")))
                .andExpect(jsonPath("$.data.examStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.integrityTag", notNullValue()));

        // 5. Verify approval history and audit logs
        mockMvc.perform(get("/api/approvals/exams/" + examId + "/history")
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));

        assertFalse(auditLogRepository.searchLogs(AuditLogService.ACTION_SUBMIT_REVIEW, "exam", examId).isEmpty());
        assertFalse(auditLogRepository.searchLogs(AuditLogService.ACTION_START_REVIEW, "exam", examId).isEmpty());
        assertFalse(auditLogRepository.searchLogs(AuditLogService.ACTION_APPROVE_EXAM, "exam", examId).isEmpty());
    }

    @Test
    @DisplayName("TASK-T3: Reject requires mandatory comment >= 10 chars and persists REJECTED state")
    void testRejectRequiresValidComment() throws Exception {
        String teacherToken = loginAndExtractToken("teacher1", "teacher123");
        String approverToken = loginAndExtractToken("approver1", "approver123");
        Long examId = createSubmittedExam(teacherToken, "Exam For Rejection");

        // Short comment -> 400 Bad Request
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/reject")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Too short\"}"))
                .andExpect(status().isBadRequest());

        // Valid comment >= 10 chars -> 200 OK, status REJECTED
        mockMvc.perform(post("/api/approvals/exams/" + examId + "/reject")
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Questions are ambiguous and need complete rework\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision", is("REJECTED")))
                .andExpect(jsonPath("$.data.examStatus", is("REJECTED")));

        assertFalse(auditLogRepository.searchLogs(AuditLogService.ACTION_REJECT_EXAM, "exam", examId).isEmpty());
    }

    @Test
    @DisplayName("TASK-T3: Database CHECK constraint prevents approver_id == creator_id")
    void testDatabaseCheckConstraintMakerChecker() {
        String uniqueCode = "CHK-" + System.currentTimeMillis();
        User teacher = userRepository.findByUsername("teacher1").orElseThrow();

        jdbcTemplate.update(
                "INSERT INTO exams (code, title, duration_minutes, created_by, entity_version, created_at, updated_at) " +
                        "VALUES (?, 'DB Constraint Test', 30, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                uniqueCode, teacher.getId()
        );
        Long examId = jdbcTemplate.queryForObject("SELECT id FROM exams WHERE code = ?", Long.class, uniqueCode);
        jdbcTemplate.update(
                "INSERT INTO exam_versions (exam_id, version_no, status, entity_version, created_at, updated_at) " +
                        "VALUES (?, 1, 'SUBMITTED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                examId
        );
        Long versionId = jdbcTemplate.queryForObject("SELECT id FROM exam_versions WHERE exam_id = ? AND version_no = 1", Long.class, examId);

        assertThrows(Exception.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO exam_approvals (exam_version_id, approver_id, creator_id, decision, comment, decided_at, created_at, updated_at) " +
                                "VALUES (?, ?, ?, 'APPROVED', 'Bypass test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        versionId, teacher.getId(), teacher.getId()
                )
        );
    }

    @Test
    @DisplayName("TASK-T7: AuditLog is INSERT-ONLY (API rejects PUT/DELETE, JPA blocks update/delete) and masks secrets")
    void testAuditLogImmutabilityAndMasking() throws Exception {
        String adminToken = loginAndExtractToken("admin", "admin123");
        User admin = userRepository.findByUsername("admin").orElseThrow();

        // 1. Masking sensitive data in AuditLog
        auditLogService.log(admin, "TEST_MASKING", "auth", 1L, AuditResult.allow,
                "Caller used Bearer eyJhbGciOiJIUzI1NiJ9.secretpayload and password=MySecretPassword123");

        List<AuditLog> maskedLogs = auditLogRepository.searchLogs("TEST_MASKING", "auth", 1L);
        assertFalse(maskedLogs.isEmpty());
        String savedReason = maskedLogs.get(0).getReason();
        assertFalse(savedReason.contains("secretpayload"), "JWT payload must be masked");
        assertFalse(savedReason.contains("MySecretPassword123"), "Password must be masked");
        assertTrue(savedReason.contains("[REDACTED]"));

        // 2. API does not allow PUT or DELETE on /api/audit-logs
        Long logId = maskedLogs.get(0).getId();
        mockMvc.perform(delete("/api/audit-logs/" + logId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(put("/api/audit-logs/" + logId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"tampered\"}"))
                .andExpect(status().is4xxClientError());

        // 3. JPA entity blocks delete/update via @PreRemove/@PreUpdate
        assertThrows(UnsupportedOperationException.class, () -> transactionTemplate.execute(status -> {
            AuditLog log = entityManager.find(AuditLog.class, logId);
            assertNotNull(log);
            entityManager.remove(log);
            entityManager.flush();
            return null;
        }));
    }

    private Long createSubmittedExam(String ownerToken, String title) throws Exception {
        MvcResult createExamResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"durationMinutes\":45,\"scorePerQuestion\":2}"))
                .andExpect(status().isOk())
                .andReturn();

        Long examId = extractExamId(createExamResult.getResponse().getContentAsString());

        String addQuestionPayload = "{\n" +
                "  \"subjectId\": " + defaultSubjectId + ",\n" +
                "  \"content\": \"Maker Checker Test Question?\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"difficulty\": \"EASY\",\n" +
                "  \"orderIndex\": 1,\n" +
                "  \"score\": 2.0,\n" +
                "  \"answers\": [\n" +
                "    {\"content\": \"Option A\", \"correct\": true},\n" +
                "    {\"content\": \"Option B\", \"correct\": false}\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(post("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addQuestionPayload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exams/" + examId + "/submit-review")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")));

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
