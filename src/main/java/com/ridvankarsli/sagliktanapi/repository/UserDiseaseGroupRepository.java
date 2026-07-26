package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroupId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDiseaseGroupRepository extends JpaRepository<UserDiseaseGroup, UserDiseaseGroupId> {

    // Kullanıcının katıldığı hastalık gruplarını listeleme (rapor 4.2)
    List<UserDiseaseGroup> findById_UserId(Long userId);

    // Hastalık grubuna üye olan kullanıcıları listeleme (rapor 4.3)
    List<UserDiseaseGroup> findById_DiseaseGroupId(Long diseaseGroupId);

    boolean existsById_UserIdAndId_DiseaseGroupId(Long userId, Long diseaseGroupId);

    void deleteById_UserIdAndId_DiseaseGroupId(Long userId, Long diseaseGroupId);
}
