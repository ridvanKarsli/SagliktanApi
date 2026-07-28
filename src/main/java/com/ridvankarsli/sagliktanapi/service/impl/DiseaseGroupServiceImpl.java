package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroupId;
import com.ridvankarsli.sagliktanapi.exception.ResourceAlreadyExistsException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.DiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.DiseaseGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiseaseGroupServiceImpl implements DiseaseGroupService {

    private final DiseaseGroupRepository diseaseGroupRepository;
    private final UserRepository userRepository;
    private final UserDiseaseGroupRepository userDiseaseGroupRepository;

    @Override
    public List<DiseaseGroup> listAll() {
        return diseaseGroupRepository.findAll();
    }

    @Override
    public DiseaseGroup getById(Long id) {
        return diseaseGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hastalık grubu bulunamadı"));
    }

    @Override
    @Transactional
    public DiseaseGroup create(String name, String description) {
        if (diseaseGroupRepository.existsByName(name)) {
            throw new ResourceAlreadyExistsException("Bu isimde bir hastalık grubu zaten var");
        }
        DiseaseGroup group = DiseaseGroup.builder()
                .name(name)
                .description(description)
                .build();
        return diseaseGroupRepository.save(group);
    }

    @Override
    @Transactional
    public DiseaseGroup update(Long id, String name, String description) {
        DiseaseGroup group = getById(id);
        group.setName(name);
        group.setDescription(description);
        return diseaseGroupRepository.save(group);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!diseaseGroupRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hastalık grubu bulunamadı");
        }
        diseaseGroupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void join(Long userId, Long diseaseGroupId) {
        if (userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(userId, diseaseGroupId)) {
            return; // zaten üye, idempotent
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        DiseaseGroup group = getById(diseaseGroupId);

        UserDiseaseGroup membership = UserDiseaseGroup.builder()
                .id(new UserDiseaseGroupId(userId, diseaseGroupId))
                .user(user)
                .diseaseGroup(group)
                .build();

        userDiseaseGroupRepository.save(membership);
    }

    @Override
    @Transactional
    public void leave(Long userId, Long diseaseGroupId) {
        userDiseaseGroupRepository.deleteById_UserIdAndId_DiseaseGroupId(userId, diseaseGroupId);
    }

    @Override
    public Page<User> listMembers(Long diseaseGroupId, Pageable pageable) {
        return userDiseaseGroupRepository.findById_DiseaseGroupIdOrderByJoinedAtAsc(diseaseGroupId, pageable)
                .map(UserDiseaseGroup::getUser);
    }

    @Override
    public long countMembers(Long diseaseGroupId) {
        return userDiseaseGroupRepository.countById_DiseaseGroupId(diseaseGroupId);
    }

    @Override
    public List<DiseaseGroup> listUserGroups(Long userId) {
        return userDiseaseGroupRepository.findById_UserId(userId)
                .stream()
                .map(UserDiseaseGroup::getDiseaseGroup)
                .toList();
    }
}
