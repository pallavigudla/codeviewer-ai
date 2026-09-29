package com.codereviewagent.config;

import com.codereviewagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("[DEV PROFILE] Database contains existing records. Skipping demo data initialization.");
            return;
        }
        log.info("[DEV PROFILE] Development profile active. Clean initial state ready.");
    }
}
