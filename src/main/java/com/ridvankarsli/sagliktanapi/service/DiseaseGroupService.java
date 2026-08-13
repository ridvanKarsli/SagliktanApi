package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DiseaseGroupService {

    List<DiseaseGroup> listAll();

    // Gruplar sayfası arama kutusu - q boş/null/anlamsızsa boş liste döner,
    // "sonuç yok" gibi görünür (çağıran taraf q boşsa zaten listAll()'a
    // yönlendirmeli, bkz. DiseaseGroupController.listAll).
    List<DiseaseGroup> search(String q);

    DiseaseGroup getById(Long id);

    DiseaseGroup create(String name, String description);

    DiseaseGroup update(Long id, String name, String description);

    void delete(Long id);

    void join(Long userId, Long diseaseGroupId);

    void leave(Long userId, Long diseaseGroupId);

    Page<User> listMembers(Long diseaseGroupId, Pageable pageable);

    long countMembers(Long diseaseGroupId);

    List<DiseaseGroup> listUserGroups(Long userId);
}
