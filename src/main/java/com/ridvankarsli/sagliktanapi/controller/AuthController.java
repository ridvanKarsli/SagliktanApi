package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.request.ChangePasswordRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ForgotPasswordRequest;
import com.ridvankarsli.sagliktanapi.dto.request.LoginRequest;
import com.ridvankarsli.sagliktanapi.dto.request.RefreshTokenRequest;
import com.ridvankarsli.sagliktanapi.dto.request.RegisterRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ResetPasswordRequest;
import com.ridvankarsli.sagliktanapi.dto.request.VerifyEmailRequest;
import com.ridvankarsli.sagliktanapi.dto.response.AuthResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.security.JwtAuthenticationFilter;
import com.ridvankarsli.sagliktanapi.service.AuthService;
import com.ridvankarsli.sagliktanapi.service.AuthTokens;
import com.ridvankarsli.sagliktanapi.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(
                authService.register(request.email(), request.password(), request.firstName(), request.lastName(),
                        request.kvkkConsent())
        );
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.email(), request.code());
        return new MessageResponse("E-posta doğrulandı");
    }

    // Mail'deki "E-postamı Doğrula" butonunun gittiği yer. Tarayıcıdan tek
    // tıkla açılabilmesi için GET — henüz frontend olmadığından backend
    // doğrudan basit bir HTML sonuç sayfası döndürüyor. Frontend kurulunca
    // bu link frontend'deki bir doğrulama sayfasına yönlendirilmeli.
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmailViaLink(@RequestParam String email, @RequestParam String code) {
        try {
            authService.verifyEmail(email, code);
            return ResponseEntity.ok(confirmationPage(true,
                    "E-posta adresiniz başarıyla doğrulandı. Artık giriş yapabilirsiniz."));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(confirmationPage(false,
                    "Doğrulama başarısız oldu: " + e.getMessage()));
        }
    }

    private String confirmationPage(boolean success, String message) {
        String color = success ? "#16a34a" : "#dc2626";
        String icon = success ? "&#10003;" : "&#10005;";
        return """
                <!DOCTYPE html>
                <html lang="tr">
                <head>
                    <meta charset="UTF-8">
                    <title>Sağlıktan - E-posta Doğrulama</title>
                    <style>
                        body { font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; background:#f3f4f6;
                               display:flex; align-items:center; justify-content:center; height:100vh; margin:0; }
                        .card { background:#fff; border-radius:12px; box-shadow:0 4px 24px rgba(0,0,0,0.08);
                                padding:40px; max-width:420px; text-align:center; }
                        .icon { width:64px; height:64px; border-radius:50%%; background:%s; color:#fff;
                                display:flex; align-items:center; justify-content:center; margin:0 auto 20px; font-size:32px; }
                        h1 { font-size:20px; color:#111827; margin:0 0 8px; }
                        p { color:#6b7280; font-size:14px; line-height:1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">%s</div>
                        <h1>SAĞLIKTAN</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(color, icon, message);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String deviceLabel = UserAgentParser.toDeviceLabel(httpRequest.getHeader("User-Agent"));
        String ipAddress = resolveClientIp(httpRequest);
        AuthTokens tokens = authService.login(request.email(), request.password(), deviceLabel, ipAddress);
        return AuthResponse.from(tokens);
    }

    // Railway/Vercel gibi proxy arkasında çalışırken gerçek istemci IP'si
    // X-Forwarded-For header'ının İLK değerinde olur (getRemoteAddr() proxy'nin
    // kendi IP'sini döner). Header yoksa (ör. lokal geliştirme) getRemoteAddr()'a
    // düşülür.
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = authService.refresh(request.refreshToken());
        return AuthResponse.from(tokens);
    }

    // Not: /api/auth/** blanket permitAll değil (SecurityConfig'e bak) —
    // bu endpoint authenticated JWT gerektirir.
    @PostMapping("/change-password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
        return new MessageResponse("Şifre değiştirildi");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return new MessageResponse("E-posta adresiniz kayıtlıysa sıfırlama kodu gönderildi");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.code(), request.newPassword());
        return new MessageResponse("Şifre sıfırlandı");
    }

    // "Aktif Oturumlar" (görev #305) öncesi burada gerçek sunucu taraflı
    // invalidation yoktu, sadece istemciye token silme hatırlatması vardı.
    // Artık bu oturuma ait refresh token DB'de revoke ediliyor (bkz.
    // AuthServiceImpl.revokeCurrentSession) - bu yüzden /auth/refresh bu
    // oturumla bir daha çalışmaz. Access token'ın kendisi stateless olduğu
    // için süresi (en fazla 1 saat) dolana kadar geçerli kalır - bu bilinen
    // ve kabul edilen bir sınır (bkz. RefreshSession ile ilgili notlar).
    @PostMapping("/logout")
    public MessageResponse logout(@AuthenticationPrincipal CustomUserDetails principal, HttpServletRequest request) {
        String sessionId = (String) request.getAttribute(JwtAuthenticationFilter.CURRENT_SESSION_ID_ATTRIBUTE);
        authService.revokeCurrentSession(principal.getId(), sessionId);
        return new MessageResponse("Çıkış yapıldı.");
    }
}
