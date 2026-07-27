package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.User;

import java.util.List;

public interface DiseaseGroupService {

    List<DiseaseGroup> listAll();

    DiseaseGroup getById(Long id);

    DiseaseGroup create(String name, String description);

    DiseaseGroup update(Long id, String name, String description);

    void delete(Long id);

    void join(Long userId, Long diseaseGroupId);

    void leave(Long userId, Long diseaseGroupId);

    List<User> listMembers(Long diseaseGroupId);

    long countMembers(Long diseaseGroupId);

    List<DiseaseGroup> listUserGroups(Long userId);
}
