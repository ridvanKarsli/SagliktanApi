package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Eski projedeki (com.saglikAdimiAPI.Helper.EmailService) SMTP bilgileri
// buraya taşındı; gönderim artık JavaMailSender üzerinden yapılıyor.
// Bu sınıf ConsoleEmailService'in (dev-only log stub) yerini alan gerçek
// implementasyondur ve tek aktif EmailService bean'idir.
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private static final String BRAND_COLOR = "#0f766e";

    private final JavaMailSender mailSender;

    @Value("${email.sender}")
    private String senderEmail;

    // Doğrulama linkinin işaret edeceği backend adresi. Henüz bir frontend
    // olmadığı için link doğrudan backend'deki GET /api/auth/verify-email
    // endpoint'ine gidiyor ve basit bir HTML sonuç sayfası dönüyor. İleride
    // gerçek bir frontend kurulunca bu, frontend'deki bir sayfaya yönlenmeli.
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("E-posta gönderildi: {} -> {}", senderEmail, to);
        } catch (MessagingException e) {
            log.error("E-posta gönderilemedi (MessagingException): {}", to, e);
        } catch (MailException e) {
            log.error("E-posta gönderilemedi (MailException, muhtemelen SMTP/kimlik doğrulama hatası): {}", to, e);
        }
    }
}
