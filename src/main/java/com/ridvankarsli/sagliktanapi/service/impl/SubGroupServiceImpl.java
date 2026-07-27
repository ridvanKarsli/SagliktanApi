package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.DiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
import com.ridvankarsli.sagliktanapi.service.SubGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubGroupServiceImpl implements SubGroupService {

    private final SubGroupRepository subGroupRepository;
    private final DiseaseGroupRepository diseaseGroupRepository;
    private final PostRepository postRepository;

    @Override
    public List<SubGroup> listByDiseaseGroup(Long diseaseGroupId) {
        return subGroupRepository.findByDiseaseGroupId(diseaseGroupId);
    }

    @Override
    public SubGroup getById(Long id) {
        return subGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alt grup bulunamadı"));
    }

    @Override
    @Transactional
    public SubGroup create(Long diseaseGroupId, String name, String description) {
        DiseaseGroup diseaseGroup = diseaseGroupRepository.findById(diseaseGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Hastalık grubu bulunamadı"));

        SubGroup subGroup = SubGroup.builder()
                .diseaseGroup(diseaseGroup)
                .name(name)
                .description(description)
                .build();

        return subGroupRepository.save(subGroup);
    }

    @Override
    @Transactional
    public SubGroup update(Long id, String name, String description) {
        SubGroup subGroup = getById(id);
        subGroup.setName(name);
        subGroup.setDescription(description);
        return subGroupRepository.save(subGroup);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!subGroupRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alt grup bulunamadı");
        }
        subGroupRepository.deleteById(id);
    }

    @Override
    public long countPosts(Long subGroupId) {
        return postRepository.countBySubGroupId(subGroupId);
    }
}
