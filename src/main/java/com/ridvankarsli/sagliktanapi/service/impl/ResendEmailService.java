package com.ridvankarsli.sagliktanapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridvankarsli.sagliktanapi.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gmail SMTP'den (eski SmtpEmailService + MailConfig, kaldırıldı) Resend'in
// REST API'sine geçiş - bkz. görev "Gmail SMTP'den transactional email
// servisine geç". Gerekçe: Gmail SMTP hem günlük gönderim limitine hem de
// kişisel bir hesabın "app password"üne bağımlıydı ve deliverability
// (gerçek kullanıcı kutusuna ulaşma, spam'e düşmeme) gerçek bir transactional
// servise göre belirgin şekilde daha zayıf. Resend'in REST API'si için ayrı
// bir SDK/bağımlılık EKLEMEDİK - java.net.http.HttpClient (JDK 11+ dahili)
// ve zaten classpath'te olan Jackson yeterli; bu yüzden spring-boot-starter-mail
// bağımlılığı da pom.xml'den kaldırıldı.
@Slf4j
@Service
public class ResendEmailService implements EmailService {

    private static final String BRAND_COLOR = "#0f766e";
    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Boş varsayılan BİLEREK: key set edilmemişse (ör. Resend henüz
    // kurulmamış bir ortam, ya da CI - orada zaten
    // app.testing.auto-verify-email=true olduğu için emailService hiç
    // çağrılmıyor, bkz. AuthServiceImpl.finalizeRegistration) uygulama yine
    // de ayağa kalkabilsin diye fail-fast yapılmıyor. send() içinde key
    // boşsa gönderim denenmez, sadece uyarı loglanır.
    @Value("${resend.api-key:}")
    private String apiKey;

    // Resend'de SADECE doğrulanmış bir domain'den gönderim yapılabiliyor.
    // Domain (sagliktan.com) doğrulanana kadar Resend'in kendi test
    // adresine (onboarding@resend.dev) düşülüyor - bu adresten SADECE
    // Resend hesabının kendi e-postasına gönderim yapılabiliyor, gerçek
    // kullanıcılara ulaşmaz. Domain doğrulanınca Railway'de
    // RESEND_FROM_EMAIL=noreply@sagliktan.com (ya da benzeri) set edilmeli.
    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    // Doğrulama linkinin işaret edeceği backend adresi (bkz. eski
    // SmtpEmailService'teki aynı not - frontend kurulunca buraya
    // yönlendirilebilir).
    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Async
    public void sendVerificationCode(String to, String code) {
        String link = baseUrl + "/api/auth/verify-email"
                + "?email=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        String html = """
                <!DOCTYPE html>
                <html lang="tr">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0; padding:0; background:#f3f4f6; font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.06);">
                        <tr><td style="background:%1$s; padding:28px; text-align:center;">
                          <span style="color:#ffffff; font-size:22px; font-weight:700; letter-spacing:0.5px;">SAĞLIKTAN</span>
                        </td></tr>
                        <tr><td style="padding:32px 36px;">
                          <h1 style="font-size:18px; color:#111827; margin:0 0 12px;">E-posta adresinizi doğrulayın</h1>
                          <p style="font-size:14px; color:#4b5563; line-height:1.6; margin:0 0 24px;">
                            SAĞLIKTAN hesabınızı aktifleştirmek için aşağıdaki butona tıklayın.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:0 auto 24px;">
                            <tr><td style="background:%1$s; border-radius:8px;">
                              <a href="%2$s" style="display:inline-block; padding:14px 32px; color:#ffffff; text-decoration:none; font-size:15px; font-weight:600;">E-postamı Doğrula</a>
                            </td></tr>
                          </table>
                          <p style="font-size:13px; color:#9ca3af; line-height:1.6; margin:0;">
                            Buton çalışmazsa bu kodu uygulamaya girebilirsiniz: <strong style="color:#111827;">%3$s</strong><br>
                            Bu kod ve bağlantı 15 dakika süreyle geçerlidir. Bu talebi siz oluşturmadıysanız bu e-postayı yok sayabilirsiniz.
                          </p>
                        </td></tr>
                        <tr><td style="padding:20px 36px; background:#f9fafb; text-align:center;">
                          <span style="font-size:12px; color:#9ca3af;">Saygılarımızla, SAĞLIKTAN Ekibi</span>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(BRAND_COLOR, link, code);

        send(to, "SAĞLIKTAN - E-posta Doğrulama", html);
    }

    @Override
    @Async
    public void sendPasswordResetCode(String to, String code) {
        String html = """
                <!DOCTYPE html>
                <html lang="tr">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0; padding:0; background:#f3f4f6; font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.06);">
                        <tr><td style="background:%1$s; padding:28px; text-align:center;">
                          <span style="color:#ffffff; font-size:22px; font-weight:700; letter-spacing:0.5px;">SAĞLIKTAN</span>
                        </td></tr>
                        <tr><td style="padding:32px 36px;">
                          <h1 style="font-size:18px; color:#111827; margin:0 0 12px;">Şifre sıfırlama kodunuz</h1>
                          <p style="font-size:14px; color:#4b5563; line-height:1.6; margin:0 0 20px;">
                            Şifrenizi sıfırlamak için aşağıdaki kodu kullanın:
                          </p>
                          <div style="text-align:center; margin:0 0 24px;">
                            <span style="display:inline-block; padding:14px 28px; background:#f0fdfa; border:1px solid %1$s; border-radius:8px; font-size:24px; font-weight:700; letter-spacing:4px; color:%1$s;">%2$s</span>
                          </div>
                          <p style="font-size:13px; color:#9ca3af; line-height:1.6; margin:0;">
                            Bu kod 15 dakika süreyle geçerlidir. Bu talebi siz oluşturmadıysanız bu e-postayı yok sayabilirsiniz.
                          </p>
                        </td></tr>
                        <tr><td style="padding:20px 36px; background:#f9fafb; text-align:center;">
                          <span style="font-size:12px; color:#9ca3af;">Saygılarımızla, SAĞLIKTAN Ekibi</span>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(BRAND_COLOR, code);

        send(to, "SAĞLIKTAN - Şifre Sıfırlama Kodu", html);
    }

    private void send(String to, String subject, String htmlBody) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY tanımlı değil, e-posta gönderilmedi (sessizce atlandı): {} -> {}", subject, to);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", "SAĞLIKTAN <" + fromEmail + ">");
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", htmlBody);

            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("E-posta gönderildi (Resend): {} -> {}", fromEmail, to);
            } else {
                // Gövdeyi de logla - Resend hata gövdesinde genelde net bir
                // sebep dönüyor (ör. "domain not verified", "invalid from").
                log.error("E-posta gönderilemedi (Resend HTTP {}): {} -> {} - gövde: {}",
                        response.statusCode(), fromEmail, to, response.body());
            }
        } catch (Exception e) {
            log.error("E-posta gönderilemedi (Resend istek hatası): {}", to, e);
        }
    }
}
