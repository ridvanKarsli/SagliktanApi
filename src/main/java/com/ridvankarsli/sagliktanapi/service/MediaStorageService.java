package com.ridvankarsli.sagliktanapi.service;

import java.util.Collection;
import java.util.Optional;

// Faz 2 adım 4: Cloudflare R2 (S3 uyumlu) ile düşük seviye, HTTP'den
// bağımsız iletişim. Bilerek "post/gönderi" kavramını bilmiyor - sadece
// storage key'lerle çalışıyor; hangi key'in hangi gönderiye ait olduğu ve
// iş kuralları (izin verilen tip/boyut/adet) PostAttachmentService'in işi
// (SRP: depolama erişimi ile "gönderiye fotoğraf ekleme" iş kuralı ayrı
// katmanlarda).
public interface MediaStorageService {

    // R2 yapılandırması (bkz. application.properties app.r2.*) eksikse
    // false döner - controller/servis katmanı bunu 503'e çevirir, uygulama
    // ayağa kalkmaya devam eder (bkz. MediaStorageServiceImpl javadoc).
    boolean isConfigured();

    // contentType MediaConstraints.ALLOWED_CONTENT_TYPES içinde değilse
    // BadRequestException fırlatır. Benzersiz bir storage key üretir,
    // sınırlı süreli (MediaConstraints.PRESIGNED_UPLOAD_TTL) bir PUT
    // linki döner - dosyanın kendisi backend'e hiç uğramadan doğrudan
    // client'tan R2'ye yüklenir.
    PresignedUpload createPresignedUpload(String contentType);

    // storageKey R2'de yoksa Optional.empty() döner (ör. kullanıcı hiç
    // yüklemeden post oluşturmayı denedi ya da geçersiz bir key gönderdi).
    Optional<ObjectMetadata> headObject(String storageKey);

    // Boş koleksiyon no-op. Toplu silme - post silindiğinde/attachment
    // listesi değiştiğinde N ayrı istek yerine tek çağrı.
    void deleteObjects(Collection<String> storageKeys);

    String publicUrlFor(String storageKey);

    record PresignedUpload(String uploadUrl, String storageKey, String publicUrl, long expiresInSeconds) {
    }

    record ObjectMetadata(String contentType, long contentLength) {
    }
}
