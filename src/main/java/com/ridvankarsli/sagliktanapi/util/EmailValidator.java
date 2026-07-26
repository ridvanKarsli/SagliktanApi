package com.ridvankarsli.sagliktanapi.util;

import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;
import java.util.regex.Pattern;

// Eski projedeki (com.saglikAdimiAPI.Helper.EmailService) regex + MX kaydı
// doğrulama mantığı buraya taşındı. Kullanıcı adı benzersizliği kontrolü
// (eski projede LogUserRepository.emailUsable) burada değil, çünkü onun
// karşılığı zaten UserRepository.existsByEmail() ile AuthServiceImpl.register()
// içinde ayrı bir adım olarak yapılıyor.
@Component
public class EmailValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public boolean isValidFormat(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // DNS üzerinden domain'in gerçekten mail alabilen (MX kaydı olan) bir
    // domain olup olmadığını kontrol eder. Not: canlı bir DNS sorgusu yapar,
    // network erişimi gerektirir ve kayıt akışını biraz yavaşlatır — eski
    // projedeki davranışla birebir aynı tutuldu.
    public boolean hasMxRecord(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1);
        try {
            DirContext ctx = new InitialDirContext(new Properties());
            Attributes attrs = ctx.getAttributes("dns:/" + domain, new String[]{"MX"});
            return attrs.get("MX") != null;
        } catch (NamingException e) {
            return false;
        }
    }

    public boolean isDeliverable(String email) {
        return isValidFormat(email) && hasMxRecord(email);
    }
}
