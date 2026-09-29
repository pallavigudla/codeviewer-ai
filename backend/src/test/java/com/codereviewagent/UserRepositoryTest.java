package com.codereviewagent;

import com.codereviewagent.entity.User;
import com.codereviewagent.entity.enums.UserRole;
import com.codereviewagent.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserRepositorySaveAndFind() {
        String email = "repo_test_" + System.currentTimeMillis() + "@codereviewagent.com";
        User user = User.builder()
                .email(email)
                .username("repo_test_" + System.currentTimeMillis())
                .passwordHash("hashed_password")
                .fullName("Repo Test User")
                .role(UserRole.DEVELOPER)
                .isEmailVerified(true)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail(email);
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("Repo Test User", found.get().getFullName());
    }
}
