package com.ridvankarsli.sagliktanapi.security;

import com.ridvankarsli.sagliktanapi.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// Filter chain seviyesinde (DispatcherServlet'e/controller'a hiç ulaşmadan)
// kimlik doğrulama başarısız olursa (token yok/geçersiz) devreye girer.
// GlobalExceptionHandler bu noktayı yakalayamaz — o yüzden aynı ErrorResponse
// formatını burada elle üretiyoruz.
// Not: Spring Boot 4.1 / Spring Framework 7, Jackson 3'e geçti — paket adı
// artık com.fasterxml.jackson değil, tools.jackson (groupId de değişti).
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), "Kimlik doğrulama gerekli");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
