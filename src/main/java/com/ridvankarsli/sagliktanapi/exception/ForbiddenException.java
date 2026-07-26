package com.ridvankarsli.sagliktanapi.exception;

// 403 - kimlik doğrulanmış ama işlem için yetkisi yok (sahiplik ihlali,
// pasif hesap, doğrulanmamış e-posta vb.)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
