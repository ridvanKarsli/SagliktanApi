package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.UserDataExportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User getById(Long id);

    User updateProfile(Long userId, String firstName, String lastName, String bio);

    void deactivate(Long userId);

    // Gelişmiş arama: ad/soyada göre kişi arama.
    Page<User> search(String query, Pageable pageable);

    // Profil sayfasındaki istatistik satırı (Gönderi/Yorum/Beğeni/Beğenmeme) -
    // bkz. UserController#getProfile / getPublicProfile.
    ProfileStats getProfileStats(Long userId);

    // KVKK veri taşınabilirliği: kullanıcının kendi ürettiği tüm veriyi
    // yapılandırılmış biçimde döner (bkz. UserController#exportMyData).
    UserDataExportResponse exportData(Long userId);

    // Hesap silme (deactivate'ten farklı - GERİ ALINAMAZ). Satır fiziksel
    // olarak silinmez: kimlik bilgisi anonimleştirilir, gönderi/yorum
    // İÇERİĞİ (comments.deleted deseniyle aynı gerekçeyle) korunur ki
    // tartışma zincirleri bozulmasın. rawPassword ile teyit zorunludur.
    void deleteAccount(Long userId, String rawPassword);

    record ProfileStats(long postCount, long commentCount, long likesReceived, long dislikesReceived) {
    }
}
