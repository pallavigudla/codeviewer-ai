package com.codereviewagent;

import com.codereviewagent.dto.CodeReviewRequestDto;
import com.codereviewagent.entity.User;
import com.codereviewagent.entity.enums.UserRole;
import com.codereviewagent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setupTestUser() {
        if (userRepository.findByEmail("developer@codereviewagent.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("developer@codereviewagent.com")
                    .username("developer_unit_test")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Developer Test")
                    .role(UserRole.DEVELOPER)
                    .isEmailVerified(true)
                    .build());
        }
    }

    @Test
    @WithMockUser(username = "developer@codereviewagent.com")
    public void testCodeReviewExecution() throws Exception {
        CodeReviewRequestDto dto = CodeReviewRequestDto.builder()
                .codeContent("public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }")
                .language("Java")
                .title("Unit Test Review")
                .build();

        mockMvc.perform(post("/api/ai/review")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallScore").exists());
    }

    @Test
    @WithMockUser(username = "developer@codereviewagent.com")
    public void testGetReviewsList() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
