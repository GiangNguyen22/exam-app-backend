package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.Role;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import com.android.app.exam_app_backend.repository.RoleRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpSecondTeacher() {
        if (userRepository.findByUsername("teacher2").isEmpty()) {
            User teacher2 = new User();
            teacher2.setUsername("teacher2");
            teacher2.setPasswordHash(passwordEncoder.encode("teacher123"));
            teacher2.setFullName("Teacher Two");
            teacher2.setEmail("teacher2@example.com");
            teacher2 = userRepository.save(teacher2);

            Role teacherRole = roleRepository.findByName("TEACHER")
                    .or(() -> roleRepository.findByName("ROLE_TEACHER"))
                    .orElse(null);
            if (teacherRole != null) {
                UserRole userRole = new UserRole();
                userRole.setUser(teacher2);
                userRole.setRole(teacherRole);
                userRoleRepository.save(userRole);
            }
        }
    }

    @Test
    @DisplayName("TASK-S4: Teacher B cannot update exam created by Teacher A (IDOR Prevention)")
    void teacherCannotUpdateOtherTeacherExam() throws Exception {
        String teacher1Token = loginAndExtractToken("teacher1", "teacher123");
        String teacher2Token = loginAndExtractToken("teacher2", "teacher123");

        Long examId = createExamAndGetId(teacher1Token, "Teacher 1 Exam");

        // Teacher 2 tries to update Teacher 1's exam
        mockMvc.perform(put("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacher2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\",\"durationMinutes\":45,\"scorePerQuestion\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thao tác trên đề thi này"));
    }

    @Test
    @DisplayName("TASK-S4: Teacher B cannot delete exam created by Teacher A")
    void teacherCannotDeleteOtherTeacherExam() throws Exception {
        String teacher1Token = loginAndExtractToken("teacher1", "teacher123");
        String teacher2Token = loginAndExtractToken("teacher2", "teacher123");

        Long examId = createExamAndGetId(teacher1Token, "Teacher 1 Exam For Delete");

        // Teacher 2 tries to delete Teacher 1's exam
        mockMvc.perform(delete("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacher2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thao tác trên đề thi này"));
    }

    @Test
    @DisplayName("TASK-S4: Teacher B cannot view questions of exam created by Teacher A")
    void teacherCannotViewQuestionsOfOtherTeacherExam() throws Exception {
        String teacher1Token = loginAndExtractToken("teacher1", "teacher123");
        String teacher2Token = loginAndExtractToken("teacher2", "teacher123");

        Long examId = createExamAndGetId(teacher1Token, "Teacher 1 Exam For Questions");

        // Teacher 2 tries to view questions of Teacher 1's exam
        mockMvc.perform(get("/api/exams/" + examId + "/questions")
                        .header("Authorization", "Bearer " + teacher2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TASK-S4: Owner and Admin can successfully update exam")
    void ownerAndAdminCanUpdateExam() throws Exception {
        String teacher1Token = loginAndExtractToken("teacher1", "teacher123");
        String adminToken = loginAndExtractToken("admin", "admin123");

        Long examId = createExamAndGetId(teacher1Token, "Owner Exam");

        // Teacher 1 (owner) can update
        mockMvc.perform(put("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated By Owner\",\"durationMinutes\":50,\"scorePerQuestion\":1}"))
                .andExpect(status().isOk());

        // Admin can also update
        mockMvc.perform(put("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated By Admin\",\"durationMinutes\":55,\"scorePerQuestion\":1}"))
                .andExpect(status().isOk());
    }

    private Long createExamAndGetId(String teacherToken, String title) throws Exception {
        MvcResult examResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"durationMinutes\":60,\"scorePerQuestion\":1}"))
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
