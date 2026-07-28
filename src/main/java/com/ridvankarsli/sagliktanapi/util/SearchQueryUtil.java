package com.ridvankarsli.sagliktanapi.util;

import java.util.regex.Pattern;

/**
 * Kullanıcı arama girdisini güvenli bir PostgreSQL "prefix tsquery"
 * string'ine çevirir (ör. "diyab tedavi" -> "diyab:* & tedavi:*").
 *
 * Bu iş bilerek Postgres tarafında (özel bir SQL fonksiyonuyla) değil,
 * burada Java'da yapılıyor. V11 migration'ındaki safe_prefix_tsquery() SQL
 * fonksiyonu canlıda arama uçlarında 500 hatasına yol açtı - kök neden
 * netleştirilemedi (aggregate SQL fonksiyonu + regconfig DEFAULT parametre
 * kombinasyonunda bir Postgres/JDBC tuhaflığı olabilir). Java'da string
 * işlemek çok daha az hareketli parça içeriyor ve doğruluğu çok daha
 * güvenilir şekilde denetlenebiliyor - veritabanına özel, canlıda
 * doğrulanmamış bir PL/SQL fonksiyonuna bağımlı kalınmıyor (bkz. V12
 * migration - eski fonksiyon oradan kaldırıldı).
 */
public final class SearchQueryUtil {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SearchQueryUtil() {
    }

    /**
     * @return Postgres'in to_tsquery(config, query) fonksiyonuna doğrudan
     *         verilebilecek, her kelimesi prefix eşleşmeli (":*") bir
     *         tsquery string'i, ya da girdi boş/anlamsızsa null - çağıran
     *         taraf null'ı "arama yapılamaz, boş sonuç dön" olarak
     *         yorumlamalı (veritabanına hiç gitmeden).
     */
    public static String toPrefixTsQuery(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        String cleaned = NON_WORD.matcher(rawInput).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        String[] words = WHITESPACE.split(cleaned);
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(" & ");
            }
            sb.append(word).append(":*");
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
