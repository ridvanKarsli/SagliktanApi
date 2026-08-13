package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// Rapor: bkz. ContentModerationService javadoc'u - küfür/spam ENGELLER,
// kriz sinyali sadece İŞARETLER. Kelime listeleri classpath'ten (bkz.
// resources/moderation/*.txt) okunur ki kod değiştirmeden güncellenebilsinler.
@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    private static final Locale TR = Locale.forLanguageTag("tr");

    // Harf olmayan her karakterden (boşluk, noktalama, rakam...) böler -
    // küfür listesi buna göre TAM KELİME eşleştirilir, aksi halde masum bir
    // kelimenin İÇİNDE geçen bir alt dize yanlışlıkla engellenebilirdi.
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}]+");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    // Aynı karakterin 7+ kez art arda tekrarı ("aaaaaaaa", "!!!!!!!!" gibi) -
    // basit ama etkili bir spam/flood belirtisi.
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{6,}");
    private static final int MAX_URLS_ALLOWED = 2;

    private static final String BLOCK_REASON =
            "İçeriğin uygunsuz veya istenmeyen (spam) kelimeler içeriyor gibi görünüyor. "
                    + "Lütfen gözden geçirip tekrar dener misin?";

    private final Set<String> bannedWords = loadEntries("moderation/banned-words-tr.txt");
    private final Set<String> crisisPhrases = loadEntries("moderation/crisis-phrases-tr.txt");

    @Override
    public ModerationResult moderate(String text) {
        if (text == null || text.isBlank()) {
            return ModerationResult.CLEAN;
        }
        String normalized = text.toLowerCase(TR);

        if (containsSpamPattern(normalized) || containsBannedWord(normalized)) {
            return new ModerationResult(true, BLOCK_REASON, false);
        }
        return new ModerationResult(false, null, containsCrisisPhrase(normalized));
    }

    private boolean containsSpamPattern(String normalized) {
        long urlCount = URL_PATTERN.matcher(normalized).results().count();
        if (urlCount > MAX_URLS_ALLOWED) {
            return true;
        }
        return REPEATED_CHAR_PATTERN.matcher(normalized).find();
    }

    private boolean containsBannedWord(String normalized) {
        for (String token : TOKEN_SPLIT.split(normalized)) {
            if (bannedWords.contains(token)) {
                return true;
            }
        }
        return false;
    }

    // Kriz ifadeleri çoğunlukla birden fazla kelimeden oluşuyor ("kendime
    // zarar" gibi), bu yüzden kelime bazlı değil tüm metin üzerinde alt dize
    // eşleşmesiyle aranıyor. Burada yanlış pozitif maliyeti düşük - bloklamıyor,
    // sadece destekleyici bir bilgi kutusu tetikliyor.
    private boolean containsCrisisPhrase(String normalized) {
        for (String phrase : crisisPhrases) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> loadEntries(String classpathResource) {
        Set<String> entries = new LinkedHashSet<>();
        for (String line : readLines(classpathResource)) {
            entries.add(line.toLowerCase(TR));
        }
        return entries;
    }

    private static List<String> readLines(String classpathResource) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(classpathResource).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Moderasyon kelime listesi okunamadı: " + classpathResource, e);
        }
        return lines;
    }
}
