package com.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //  Disable CSRF (Essential for REST APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // Set Session Management to Stateless
                // This ensures the server doesn't store session IDs, forcing
                // the client to send the Basic Auth header with every request.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Optional: Allow anyone to check if the app is running
                        .requestMatchers("/actuator/health").permitAll()

                        // Protect your query endpoints
                        // Note: hasRole("ANALYST") checks for "ROLE_ANALYST" in database
                        .requestMatchers("/api/queries/**").hasRole("ANALYST")

                        // Lock down everything else
                        .anyRequest().authenticated()
                )

                // Enable HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // This looks for users in 'users' table and roles in 'authorities' table automatically.
    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    // This matches the encoder used in your UserManagingService.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
