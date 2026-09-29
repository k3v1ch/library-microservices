package com.library.book.config;

import com.library.common.security.SecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Внутренний API — только для сервисов: токен client_credentials со scope=internal.
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_" + SecurityDefaults.SCOPE_INTERNAL)
                        // Поиск по каталогу открыт всем: читателю не нужен аккаунт, чтобы посмотреть фонд.
                        .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                        // Менять каталог может только библиотекарь.
                        .requestMatchers("/books/**").hasRole(SecurityDefaults.ROLE_LIBRARIAN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(SecurityDefaults.jwtAuthenticationConverter())))
                .build();
    }
}
