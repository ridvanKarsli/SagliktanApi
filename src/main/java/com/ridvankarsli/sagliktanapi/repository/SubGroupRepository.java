package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubGroupRepository extends JpaRepository<SubGroup, Long> {

    List<SubGroup> findByDiseaseGroupId(Long diseaseGroupId);
}
