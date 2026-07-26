package com.ridvankarsli.sagliktanapi.exception;

// 404 - istenen kayıt bulunamadı (kullanıcı, hastalık grubu, gönderi vb.)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
