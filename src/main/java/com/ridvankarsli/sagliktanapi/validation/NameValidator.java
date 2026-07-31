package com.ridvankarsli.sagliktanapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// bkz. ValidName - iki aşamalı kontrol:
// 1) Yapısal: sadece harf + boşluk/tire/kesme işareti, harfle başlar/biter,
//    art arda ayraç yok (" - Ahmet" ya da "Ahmet--Kaya" gibi girişler düşer).
// 2) Anlamsal (kısmi): bilinen "şaka"/placeholder kelimelerini reddet.
//    Bu liste ASLA tam olamaz (bkz. ValidName'deki not) - amaç en yaygın
//    bariz girişleri elemek, geri kalanı admin panelinden elle temizlemek.
public class NameValidator implements ConstraintValidator<ValidName, String> {

    // \p{L}: Unicode "harf" kategorisi - Türkçe (ğ,ü,ş,ı,ö,ç) dahil her
    // dilin harfini kapsar, elle alfabe listelemekten daha sağlam.
    // Yapı: harf(ler) + (ayraç + harf(ler))* - yani "Ayşe-Nur", "O'Connor",
    // "Ali Veli" gibi girişlere izin verir ama başta/sonda veya art arda
    // ayraca izin vermez.
    private static final Pattern STRUCTURE = Pattern.compile("^\\p{L}+(?:[ '\\-]\\p{L}+)*$");

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    // Bilinen placeholder/şaka girişleri - küçük harfe çevrilip (Türkçe
    // locale ile, "İ"/"I" karışıklığını önlemek için) karşılaştırılır.
    // Genişletilebilir bir başlangıç listesi, tam kapsamlı olması beklenmez.
    private static final Set<String> BANNED_WORDS = Set.of(
            "test", "deneme", "asdf", "qwerty", "yok", "bilinmiyor",
            "isimyok", "mal", "salak", "aptal", "xxx", "abc", "isim", "soyisim"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // boş/null kontrolü @NotBlank'ın işi, burada karışmıyoruz
        }

        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return false;
        }

        if (!STRUCTURE.matcher(trimmed).matches()) {
            return false;
        }

        String normalized = trimmed.toLowerCase(Locale.forLanguageTag("tr"));
        // Hem tüm alanı (ör. "test") hem alan içindeki her kelimeyi
        // (ör. "test kullanıcı") ayrı ayrı yasaklı listeye karşı kontrol et.
        if (BANNED_WORDS.contains(normalized)) {
            return false;
        }
        for (String word : normalized.split("[ '\\-]+")) {
            if (BANNED_WORDS.contains(word)) {
                return false;
            }
        }

        return true;
    }
}
