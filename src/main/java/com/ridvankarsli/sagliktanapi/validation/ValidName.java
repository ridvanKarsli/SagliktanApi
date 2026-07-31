package com.ridvankarsli.sagliktanapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Ad/soyad alanları için: sadece harf (Türkçe dahil, \p{L} ile herhangi bir
// dilin harfi), boşluk, tire, kesme işareti - rakam/sembol/emoji yok. Ayrıca
// bilinen "şaka" girişlerini (test, mal, salak vb.) reddeder - bkz.
// NameValidator. Kasıtlı olarak gevşek tutuldu: amaç "gerçek bir isim mi"yi
// kanıtlamak değil (bu hesaplanabilir bir şey değil - bkz. "Falsehoods
// Programmers Believe About Names"), sadece bariz saçmalığı elemek.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameValidator.class)
public @interface ValidName {
    String message() default "Geçerli bir isim giriniz";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
