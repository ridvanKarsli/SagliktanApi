package com.ridvankarsli.sagliktanapi.util;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Faz 2 adım 4: gönderi fotoğrafları için ortak kısıtlar. Hem presigned URL
 * üretilirken (MediaStorageServiceImpl - "hangi content-type için üretim
 * yapılabilir") hem de yüklenen dosya bir gönderiye bağlanırken
 * (PostAttachmentServiceImpl - "gerçekten izin verilen tipte/boyutta mı")
 * aynı kurallar uygulanmalı - tek yerde tanımlanıp paylaşılıyor (DRY),
 * ikisi birbirinden bağımsız evrilip tutarsız hale gelmesin diye.
 */
public final class MediaConstraints {

    private MediaConstraints() {
    }

    // Video bilerek kapsam dışı (bkz. PLAN_faz2_ozellikler.md - "İlk fazda
    // YOK, sadece fotoğraf"). Değer -> dosya uzantısı, presigned URL'in
    // storage key'ini üretirken kullanılıyor.
    public static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    // Client-side sıkıştırma (~1920px, %75-80 kalite) sonrası tipik bir
    // fotoğraf birkaç yüz KB civarında kalıyor - 8MB payı bolca güvenlik
    // marjı bırakıyor, asıl savunma client-side sıkıştırma + burada
    // sunucu tarafı doğrulama (bkz. PostAttachmentServiceImpl).
    public static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024;

    public static final int MAX_ATTACHMENTS_PER_POST = 6;

    // Kullanıcının fotoğrafı seçip yüklemesi için makul bir süre - çok uzun
    // tutulursa imzalı URL gereksiz yere uzun süre geçerli kalır.
    public static final Duration PRESIGNED_UPLOAD_TTL = Duration.ofMinutes(10);

    public static boolean isAllowedContentType(String contentType) {
        return contentType != null && ALLOWED_CONTENT_TYPES.containsKey(contentType);
    }

    public static Set<String> allowedContentTypes() {
        return ALLOWED_CONTENT_TYPES.keySet();
    }
}
