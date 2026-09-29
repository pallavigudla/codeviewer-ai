package com.codereviewagent.config;

import com.codereviewagent.entity.User;
import com.codereviewagent.entity.enums.UserRole;
import com.codereviewagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String demoEmail = "demo@codereviewagent.com";
        if (userRepository.findByEmail(demoEmail).isEmpty()) {
            User demoUser = User.builder()
                    .email(demoEmail)
                    .username("demo")
                    .passwordHash(passwordEncoder.encode("Demo@12345"))
                    .fullName("Demo Developer")
                    .role(UserRole.DEVELOPER)
                    .isEmailVerified(true)
                    .build();

            userRepository.save(demoUser);
            log.info("Default seed user demo account created: [{}]", demoEmail);
        }
    }
}
