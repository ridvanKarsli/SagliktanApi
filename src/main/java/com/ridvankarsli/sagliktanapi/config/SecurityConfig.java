package com.ridvankarsli.sagliktanapi.config;

import com.ridvankarsli.sagliktanapi.security.CustomUserDetailsService;
import com.ridvankarsli.sagliktanapi.security.JwtAuthenticationFilter;
import com.ridvankarsli.sagliktanapi.security.RateLimitFilter;
import com.ridvankarsli.sagliktanapi.security.RestAccessDeniedHandler;
import com.ridvankarsli.sagliktanapi.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

// Rapor adım 5: Spring Security temel konfigürasyonu + JWT filter zinciri.
// JWT tabanlı stateless API olduğu için CSRF açıkça devre dışı bırakıldı
// (Spring Boot 4.1 / Spring Security 7 varsayılan davranışı).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Frontend'i vercel.json rewrite ile same-origin proxy'lediğimiz için
    // ana akış CORS'a hiç düşmüyor, ama Swagger'dan doğrudan farklı bir
    // origin'den denenen istekler ve ileride gelebilecek başka istemciler
    // (mobil app vb.) için açık bir allowlist tanımlıyoruz - joker (*) yok.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://sagliktan.com",
                "https://www.sagliktan.com",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Dikkat: /api/auth/** blanket permitAll DEĞİL — sadece
                        // gerçekten public olan uçlar burada. change-password ve
                        // logout authenticated JWT gerektiriyor (aşağıdaki
                        // anyRequest().authenticated() ile korunuyor).
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        // Swagger UI + OpenAPI JSON/YAML (rapor adım 11)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        // Railway health check bu endpoint'e authsuz istek atıyor;
                        // başka hiçbir actuator endpoint'i public değil.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Filter chain seviyesindeki 401/403'ler de GlobalExceptionHandler'daki
                // ile aynı ErrorResponse JSON formatında dönsün diye (rapor adım 10).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Rate limit kontrolü JWT doğrulamasından bile önce çalışsın -
                // gereksiz token doğrulama maliyetine girmeden en erken reddedilsin.
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
