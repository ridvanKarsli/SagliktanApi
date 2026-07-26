package com.ridvankarsli.sagliktanapi.exception;

// 409 - benzersizlik ihlali (kayıtlı e-posta, aynı isimde hastalık grubu vb.)
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
