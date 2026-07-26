package com.ridvankarsli.sagliktanapi.exception;

// 400 - istek kendi içinde geçersiz (yanlış/süresi geçmiş kod, geçersiz e-posta vb.)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
