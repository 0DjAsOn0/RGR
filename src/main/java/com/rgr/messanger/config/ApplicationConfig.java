package com.rgr.messanger.config;

import com.rgr.messanger.web.security.JwtTokenFilter;
import com.rgr.messanger.web.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor(onConstructor_ = @__(@Lazy))
public class ApplicationConfig {

    private final JwtTokenProvider tokenProvider;

    private static final String[] PUBLIC_PAGES = {
            "/", "/login", "/register", "/passreset", "/check-email"
    };

    private static final String[] STATIC_RESOURCES = {
            "/styles/**", "/js/**", "/img/**", "/icons/**", "/avatars/**"
    };

    private static final String[] PUBLIC_API = {
            "/api/v1/auth/**",
            "/swagger-ui/**", "/v3/api-docs/**", "/graphiql",
            "/ws/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PAGES).permitAll()
                        .requestMatchers(STATIC_RESOURCES).permitAll()
                        .requestMatchers(PUBLIC_API).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtTokenFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint authEntryPoint() {
        return (request, response, ex) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Unauthorized.");
            } else {
                response.sendRedirect("/login");
            }
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Forbidden.");
        };
    }
}
