package com.ridvankarsli.sagliktanapi.config;

import com.ridvankarsli.sagliktanapi.security.CustomUserDetailsService;
import com.ridvankarsli.sagliktanapi.security.JwtAuthenticationFilter;
import com.ridvankarsli.sagliktanapi.security.RateLimitFilter;
import com.ridvankarsli.sagliktanapi.security.RestAccessDeniedHandler;
import com.ridvankarsli.sagliktanapi.security.RestAuthenticationEntryPoint;
import com.ridvankarsli.sagliktanapi.util.CorsOrigins;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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

    // app.cors.allowed-origins -> ALLOWED_ORIGINS env degiskeni (bkz.
    // application.properties). Prod'da bu deger set edilmiyor, varsayilan
    // (sagliktan.com) kullaniliyor - davranis eskisiyle ayni. Staging
    // ortaminda Vercel preview domain'i de bu listeye eklenecek.
    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

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
        config.setAllowedOrigins(parseAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // WebSocketConfig da ayni ALLOWED_ORIGINS degerini kullaniyor (orada
    // ayri enjekte ediliyor - Spring CORS filter'indan tamamen farkli bir
    // mekanizma oldugu icin bean paylasimi yapilamiyor, ama parse mantigi
    // CorsOrigins.parse'ta ortak).
    List<String> parseAllowedOrigins() {
        return CorsOrigins.parse(allowedOriginsRaw);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Faz 3-2a: HTTP güvenlik başlıkları. frameOptions/contentTypeOptions/
                // HSTS Spring Security'de zaten varsayılan olarak açık, ama sürüm
                // farklılıklarına bağlı kalmamak için burada AÇIKÇA tanımlıyoruz -
                // "muhtemelen zaten açık" bir güvenlik denetiminde kabul edilebilir
                // bir varsayım değil. Bu API neredeyse tamamen JSON döner; tek istisna
                // /api/auth/verify-email (GET) - basit bir inline-style'lı HTML onay
                // sayfası (bkz. AuthController#confirmationPage), dış kaynak
                // yüklemiyor - CSP'yi ona göre en kısıtlayıcı şekilde ayarladık.
                // Not: bu bloktaki her satır bilerek AYRI bir "headers.xxx(...)" çağrısı
                // olarak yazıldı, birbirine ZİNCİRLEME (chained) değil - permissionsPolicy(...)
                // bu Spring Security sürümünde (deprecated/kaldırılmak üzere) HeadersConfigurer
                // DEĞİL kendi alt-config tipini (PermissionsPolicyConfig) döndürüyor, bu yüzden
                // ardından .contentSecurityPolicy(...) zincirlemek derleme hatası veriyordu.
                // Her çağrıyı doğrudan "headers" değişkeni üzerinden yaparak dönüş tipinden
                // bağımsız, sürüm değişikliklerine karşı daha dayanıklı hale getirdik.
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(permissions -> permissions
                            .policy("geolocation=(), camera=(), microphone=(), payment=(), usb=()"));
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'"));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                })
                // KRİTİK: Spring Security varsayılan olarak token'sız her isteğe bir
                // AnonymousAuthenticationToken atar ve bu token'ın isAuthenticated()'ı
                // true döner - bu yüzden anonymous() devre dışı bırakılmadan
                // .anyRequest().authenticated() token'sız istekleri de GEÇİRİYORDU.
                // (JwtAuthenticationFilter token yoksa SecurityContext'e hiçbir şey
                // set etmiyor - o boşluğu Spring'in kendi AnonymousAuthenticationFilter'ı
                // dolduruyordu.) Anonymous'u tamamen kapatınca token'sız istekte
                // Authentication null kalıyor ve .authenticated() doğru şekilde
                // reddedip restAuthenticationEntryPoint üzerinden temiz bir 401 dönüyor.
                .anonymous(AbstractHttpConfigurer::disable)
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
                        // WebSocket (STOMP) el sıkışma isteği: tarayıcının native
                        // WebSocket API'si Authorization header'ı taşıyamıyor, bu
                        // yüzden gerçek JWT doğrulaması burada değil, ilk STOMP
                        // CONNECT frame'inde yapılıyor (bkz.
                        // JwtHandshakeChannelInterceptor). Bu satır sadece ham HTTP
                        // upgrade isteğinin buraya kadar ulaşmasına izin verir.
                        .requestMatchers("/ws/**").permitAll()
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
