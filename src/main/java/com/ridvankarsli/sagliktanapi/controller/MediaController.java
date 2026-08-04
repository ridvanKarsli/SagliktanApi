package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.request.PresignedUploadRequest;
import com.ridvankarsli.sagliktanapi.dto.response.PresignedUploadResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Faz 2 adım 4: gönderi fotoğrafları için R2'ye doğrudan (backend'i atlayarak)
// yükleme akışının ilk adımı. principal burada storageKey'e dahil edilmiyor
// (bkz. MediaStorageServiceImpl - key sadece "posts/{uuid}.{ext}") çünkü
// yüklenen dosyanın gerçekten hangi posta ait olacağı henüz belli değil -
// asıl sahiplik/ilişkilendirme post oluşturma isteğinde (bkz.
// PostAttachmentService.attach) kuruluyor. principal parametresi yine de
// tutuluyor: @AuthenticationPrincipal olmadan bu uca kimliksiz erişim
// mümkün olurdu (SecurityConfig'te anyRequest().authenticated() zaten
// bunu engelliyor, ama parametre burada olması gereği açıkça belgeliyor).
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping("/api/media/presigned-upload-url")
    public PresignedUploadResponse createPresignedUploadUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PresignedUploadRequest request
    ) {
        return PresignedUploadResponse.from(mediaStorageService.createPresignedUpload(request.contentType()));
    }
}
