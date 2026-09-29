package com.codereviewagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookiePath("/");

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(tokenRepository)
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                        "/api/auth/register", "/register",
                        "/api/auth/verify-email", "/verify-email",
                        "/api/auth/resend-otp", "/resend-otp",
                        "/api/auth/login", "/login",
                        "/api/auth/forgot-password", "/forgot-password",
                        "/api/auth/verify-reset-otp", "/verify-reset-otp",
                        "/api/auth/reset-password", "/reset-password"
                )
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                // Public Static Web Resources
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/dashboard.html",
                                 "/review.html", "/history.html", "/memory.html", "/settings.html",
                                 "/css/**", "/js/**", "/assets/**", "/favicon.ico", "/error").permitAll()

                // Public Authentication Endpoints
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/register",
                        "/api/auth/verify-email", "/verify-email",
                        "/api/auth/resend-otp", "/resend-otp",
                        "/api/auth/login", "/login",
                        "/api/auth/forgot-password", "/forgot-password",
                        "/api/auth/verify-reset-otp", "/verify-reset-otp",
                        "/api/auth/reset-password", "/reset-password"
                ).permitAll()

                // Require Authentication for All Other API & System Endpoints
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = Map.of(
                            "timestamp", LocalDateTime.now().toString(),
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Authentication required. Please log in.",
                            "path", request.getRequestURI()
                    );
                    new ObjectMapper().writeValue(response.getOutputStream(), body);
                })
            );

        return http.build();
    }

    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
