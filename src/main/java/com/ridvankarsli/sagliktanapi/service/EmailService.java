package com.ridvankarsli.sagliktanapi.service;

// SMTP entegrasyonundan bağımsız soyutlama. Şu an dev ortamı için log'a yazan
// bir implementasyonu var (ConsoleEmailService); ileride gerçek SMTP/3.parti
// servis (SendGrid vb.) implementasyonuyla değiştirilebilir, service katmanı
// bundan etkilenmez.
public interface EmailService {

    void sendVerificationCode(String to, String code);

    void sendPasswordResetCode(String to, String code);
}
