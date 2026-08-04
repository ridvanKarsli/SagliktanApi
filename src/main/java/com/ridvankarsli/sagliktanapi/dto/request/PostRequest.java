package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        // Faz 2 adım 4: MediaStorageService.createPresignedUpload ile
        // üretilip client tarafından R2'ye zaten yüklenmiş storage
        // key'leri. Sadece post OLUŞTURULURKEN dikkate alınır - update
        // isteklerinde bu alan yok sayılır (bkz. PostController.update).
        @Size(max = 6, message = "Bir gönderiye en fazla 6 fotoğraf eklenebilir")
        List<String> attachmentKeys
) {
}
