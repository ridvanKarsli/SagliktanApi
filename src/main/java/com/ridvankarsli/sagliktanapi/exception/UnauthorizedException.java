package com.ridvankarsli.sagliktanapi.exception;

// 401 - kimlik doğrulama başarısız (yanlış şifre, geçersiz/süresi dolmuş token)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
