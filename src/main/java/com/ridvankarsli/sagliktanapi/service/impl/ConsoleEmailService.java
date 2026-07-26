package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.service.EmailService;
import lombok.extern.slf4j.Slf4j;

// ARTIK BEAN DEĞİL (@Service kaldırıldı): SMTP entegrasyonu (SmtpEmailService)
// tamamlandığı için devre dışı bırakıldı. SMTP bilgisi olmadan yerel
// denemeler yapmak istersen @Service'i buraya geri koyup SmtpEmailService'ten
// kaldırman yeterli — iki tanesi aynı anda bean olursa Spring "ambiguous
// EmailService bean" hatası verir.
@Slf4j
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("[DEV-MAIL] {} adresine e-posta doğrulama kodu gönderildi: {}", to, code);
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        log.info("[DEV-MAIL] {} adresine şifre sıfırlama kodu gönderildi: {}", to, code);
    }
}
