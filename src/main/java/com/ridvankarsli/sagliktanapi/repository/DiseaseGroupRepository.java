package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiseaseGroupRepository extends JpaRepository<DiseaseGroup, Long> {

    Optional<DiseaseGroup> findByName(String name);

    boolean existsByName(String name);
}
