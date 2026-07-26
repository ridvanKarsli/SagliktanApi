package com.ridvankarsli.sagliktanapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

// SMTP ayarları eski projedeki (com.saglikAdimiAPI.Helper.EmailService) ile
// aynı property isimleriyle (email.sender / email.password / email.smtp.host /
// email.smtp.port) okunuyor, sadece gönderim Spring'in JavaMailSender'ı
// üzerinden (raw Session/Transport yerine) yapılıyor.
@Configuration
public class MailConfig {

    @Value("${email.smtp.host}")
    private String host;

    @Value("${email.smtp.port}")
    private int port;

    @Value("${email.sender}")
    private String senderEmail;

    @Value("${email.password}")
    private String senderPassword;

    @Bean
    public JavaMailSenderImpl javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(senderEmail);
        mailSender.setPassword(senderPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
