package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.util.MediaConstraints;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Faz 2 adım 4: Cloudflare R2, S3 API'siyle uyumlu olduğu için AWS SDK v2
// (software.amazon.awssdk:s3) buraya endpointOverride ile yönlendirilerek
// kullanılıyor - ayrı bir R2-özel kütüphaneye gerek yok (bkz. Cloudflare'in
// resmi "aws-sdk-java" örneği).
//
// R2 ayarları (bkz. application.properties app.r2.*) EKSİK OLABİLİR -
// örneğin bu özellik henüz kurulmamış bir ortamda. Diğer sırların
// (app.jwt.secret, email.password) aksine bilerek fail-fast yapılmadı:
// bu tamamen opsiyonel bir alt özellik, eksikliği tüm API'nin ayağa
// kalkmasını engellememeli. Bunun yerine client (S3Client/S3Presigner)
// @PostConstruct'ta SADECE yapılandırma tamsa kurulur; eksikse alan null
// kalır ve her metot en başta isConfigured() kontrolüyle temiz bir 503
// döner.
@Slf4j
@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    @Value("${app.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${app.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.r2.endpoint:}")
    private String endpoint;

    @Value("${app.r2.bucket:}")
    private String bucket;

    @Value("${app.r2.public-base-url:}")
    private String publicBaseUrl;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    void init() {
        if (!isConfigured()) {
            log.warn("Cloudflare R2 yapılandırılmamış (app.r2.* eksik) - medya yükleme uçları 503 dönecek");
            return;
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        URI endpointUri = URI.create(endpoint);
        // R2 gerçek bir AWS region'ı kullanmıyor ama SDK imzalama (SigV4)
        // için bir region değeri istiyor - Cloudflare'in kendi örneğindeki
        // gibi "auto" veriliyor, sadece SDK'yı tatmin etmek için.
        Region region = Region.of("auto");

        this.s3Client = S3Client.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(credentialsProvider)
                .region(region)
                // chunkedEncodingEnabled(false): SDK v2 varsayılan olarak
                // putObject'te chunked transfer encoding kullanıyor, bu R2
                // ile imza uyuşmazlığı (403) yaratıyor - Cloudflare'in
                // dokümantasyonundaki zorunlu ayar.
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @PreDestroy
    void shutdown() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }

    @Override
    public boolean isConfigured() {
        return isNotBlank(accessKeyId) && isNotBlank(secretAccessKey) && isNotBlank(endpoint)
                && isNotBlank(bucket) && isNotBlank(publicBaseUrl);
    }

    @Override
    public PresignedUpload createPresignedUpload(String contentType) {
        requireConfigured();
        if (!MediaConstraints.isAllowedContentType(contentType)) {
            throw new BadRequestException("Desteklenmeyen dosya tipi: " + contentType);
        }
        String extension = MediaConstraints.ALLOWED_CONTENT_TYPES.get(contentType);
        String storageKey = "posts/" + UUID.randomUUID() + "." + extension;

        // contentType burada imzaya dahil ediliyor (PutObjectRequest.
        // contentType) - bu, client'ın PUT sırasında AYNI Content-Type
        // header'ını göndermek ZORUNDA olduğu anlamına gelir (SigV4 imza
        // uyuşmazlığında 403 döner), böylece R2'ye yüklenen dosyanın
        // gerçek Content-Type'ı bizim izin verdiğimiz değerle garantiye
        // alınmış oluyor.
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(MediaConstraints.PRESIGNED_UPLOAD_TTL)
                .putObjectRequest(b -> b.bucket(bucket).key(storageKey).contentType(contentType))
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        return new PresignedUpload(
                presigned.url().toString(),
                storageKey,
                publicUrlFor(storageKey),
                MediaConstraints.PRESIGNED_UPLOAD_TTL.toSeconds()
        );
    }

    @Override
    public Optional<ObjectMetadata> headObject(String storageKey) {
        requireConfigured();
        try {
            HeadObjectResponse response = s3Client.headObject(b -> b.bucket(bucket).key(storageKey));
            return Optional.of(new ObjectMetadata(response.contentType(), response.contentLength()));
        } catch (S3Exception e) {
            // HeadObject'te gövde olmadığı için S3/R2 404'te modellenmiş
            // NoSuchKeyException yerine genelde ham S3Exception döner -
            // status koduna bakmak gerekiyor.
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void deleteObjects(Collection<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }
        if (!isConfigured()) {
            // Post silme akışını R2 yapılandırma sorunu yüzünden
            // engellemek istemiyoruz - en kötü ihtimalle orphan bir
            // nesne kalır, bu DB tutarlılığından daha az kritik.
            log.warn("R2 yapılandırılmamışken deleteObjects çağrıldı, atlanıyor: {}", storageKeys);
            return;
        }
        try {
            List<ObjectIdentifier> ids = storageKeys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(ids).build())
                    .build());
        } catch (S3Exception e) {
            // Best-effort temizlik: R2 tarafı geçici olarak erişilemez olsa
            // bile asıl işlemin (post silme) tamamlanmasını engellemiyoruz.
            log.error("R2 nesneleri silinemedi: {}", storageKeys, e);
        }
    }

    @Override
    public String publicUrlFor(String storageKey) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/" + storageKey;
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Medya yükleme şu anda kullanılamıyor");
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
