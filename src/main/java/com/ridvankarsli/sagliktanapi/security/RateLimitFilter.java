package com.ridvankarsli.sagliktanapi.security;

import com.ridvankarsli.sagliktanapi.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

// Kimlik doğrulama gerektirmeyen, kötüye kullanıma açık uçlara (kayıt,
// login, şifre sıfırlama) IP bazlı basit rate limiting uygular. Amaç
// brute-force login denemelerini ve kayıt/e-posta spam'ini engellemek.
// Not: RestAuthenticationEntryPoint'teki gibi, bu filter DispatcherServlet'e
// hiç ulaşmadan çalıştığı için GlobalExceptionHandler'ı kullanamaz, aynı
// ErrorResponse formatını elle üretiyoruz.
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    private record Rule(String path, String method, int limit, Duration window) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("/api/auth/register", "POST", 5, Duration.ofMinutes(15)),
            new Rule("/api/auth/login", "POST", 10, Duration.ofMinutes(15)),
            new Rule("/api/auth/forgot-password", "POST", 3, Duration.ofMinutes(15)),
            new Rule("/api/auth/reset-password", "POST", 5, Duration.ofMinutes(15))
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Rule matched = RULES.stream()
                .filter(r -> r.method().equalsIgnoreCase(request.getMethod())
                        && r.path().equals(request.getRequestURI()))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + matched.path();
        if (!rateLimiter.tryConsume(key, matched.limit(), matched.window())) {
            HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
            response.setStatus(status.value());
            response.setContentType("application/json;charset=UTF-8");
            ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(),
                    "Çok fazla istek gönderildi, lütfen biraz sonra tekrar deneyin");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Railway gibi bir proxy'nin arkasında çalıştığımız için gerçek istemci
    // IP'si X-Forwarded-For header'ında gelir; yoksa remoteAddr'a düşülür.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
