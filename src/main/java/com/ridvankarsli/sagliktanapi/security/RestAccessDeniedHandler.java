package com.ridvankarsli.sagliktanapi.security;

import com.ridvankarsli.sagliktanapi.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// Kimliği doğrulanmış ama yetkisiz kullanıcı, filter chain seviyesinde
// (ör. authorizeHttpRequests kuralına takılan) erişim reddedilirse devreye girer.
// Not: Spring Boot 4.1 / Spring Framework 7, Jackson 3'e geçti — paket adı
// artık com.fasterxml.jackson değil, tools.jackson.
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), "Bu işlem için yetkiniz yok");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
