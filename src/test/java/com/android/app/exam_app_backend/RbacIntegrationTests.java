package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("student1").isEmpty()) {
            User student = new User();
            student.setUsername("student1");
            student.setPasswordHash(passwordEncoder.encode("student123"));
            student.setFullName("Student One");
            student.setEmail("student1@example.com");
            userRepository.save(student);
        }
    }

    @Test
    void shouldRejectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/rbac/question-create-check"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldAllowAdminWithPermission() throws Exception {
        String adminToken = loginAndExtractToken("admin", "admin123");

        mockMvc.perform(get("/api/rbac/question-create-check")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyUserWithoutPermission() throws Exception {
        String studentToken = loginAndExtractToken("student1", "student123");

        mockMvc.perform(get("/api/rbac/question-create-check")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
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
