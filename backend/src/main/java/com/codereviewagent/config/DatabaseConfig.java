package com.codereviewagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.codereviewagent.repository")
public class DatabaseConfig {
    // Database configuration and JPA initialization beans
}
