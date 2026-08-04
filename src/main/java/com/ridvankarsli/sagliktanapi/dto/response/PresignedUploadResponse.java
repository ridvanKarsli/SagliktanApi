package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.service.MediaStorageService;

// uploadUrl: client'ın PUT ile dosyayı doğrudan R2'ye yükleyeceği imzalı
// link (backend'e hiç uğramaz). storageKey: yükleme bittikten sonra post
// oluşturma isteğinde (PostRequest.attachmentKeys) geri gönderilmesi
// gereken referans. publicUrl: önizleme için - dosya henüz yüklenmemiş
// olsa da URL'in kendisi deterministik, önizlemede kullanılabilir.
public record PresignedUploadResponse(String uploadUrl, String storageKey, String publicUrl, long expiresInSeconds) {

    public static PresignedUploadResponse from(MediaStorageService.PresignedUpload upload) {
        return new PresignedUploadResponse(
                upload.uploadUrl(), upload.storageKey(), upload.publicUrl(), upload.expiresInSeconds());
    }
}
