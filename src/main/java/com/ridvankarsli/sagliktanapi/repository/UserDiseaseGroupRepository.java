package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroupId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDiseaseGroupRepository extends JpaRepository<UserDiseaseGroup, UserDiseaseGroupId> {

    // Kullanıcının katıldığı hastalık gruplarını listeleme (rapor 4.2)
    List<UserDiseaseGroup> findById_UserId(Long userId);

    // Hastalık grubuna üye olan kullanıcıları listeleme (rapor 4.3) - üye
    // sayısı büyüyebileceği için (1000+ kullanıcı hedefi) sayfalı çekiliyor,
    // bkz. DiseaseGroupController.listMembers. OrderByJoinedAtAsc: en eski
    // üye en üstte - ayrıca ORDER BY olmadan sayfalar arası sıra garanti
    // edilmez.
    Page<UserDiseaseGroup> findById_DiseaseGroupIdOrderByJoinedAtAsc(Long diseaseGroupId, Pageable pageable);

    // Grup listesinde/detayında gösterilen üye sayısı
    long countById_DiseaseGroupId(Long diseaseGroupId);

    boolean existsById_UserIdAndId_DiseaseGroupId(Long userId, Long diseaseGroupId);

    void deleteById_UserIdAndId_DiseaseGroupId(Long userId, Long diseaseGroupId);
}
