package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.SubGroup;

import java.util.List;

public interface SubGroupService {

    List<SubGroup> listByDiseaseGroup(Long diseaseGroupId);

    SubGroup getById(Long id);

    SubGroup create(Long diseaseGroupId, String name, String description);

    SubGroup update(Long id, String name, String description);

    void delete(Long id);

    long countPosts(Long subGroupId);
}
