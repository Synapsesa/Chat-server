package com.synapse.chat_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;

import com.synapse.chat_service.filter.CustomAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final CustomAuthenticationFilter customAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // WebSocket 핸드셰이크 경로 ("/ws/**") - 인증된 사용자만 허용
                        .requestMatchers("/ws/**").authenticated()
                        // 채팅 관련 API ("/api/v1/messages/**", "/api/v1/ai-chat/**") - 인증된 사용자만 허용
                        .requestMatchers("/api/v1/messages/**", "/api/v1/ai-chat/**").authenticated()
                        // CSRF 토큰 발급 API ("/api/v1/csrf-token") - 인증된 사용자만 허용
                        .requestMatchers("/api/v1/csrf-token").authenticated()
                        .requestMatchers("/api/internal/**").access(AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.hasAuthority("SCOPE_api.internal"),
                                AuthorityAuthorizationManager.hasAuthority("SCOPE_chat:read")
                        ))
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
