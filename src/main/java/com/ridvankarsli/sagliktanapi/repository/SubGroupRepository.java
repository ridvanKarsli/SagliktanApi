package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SubGroupRepository extends JpaRepository<SubGroup, Long> {

    List<SubGroup> findByDiseaseGroupId(Long diseaseGroupId);

    // Ana sayfa karışık akışı (bkz. PostResponseAssembler.assembleFeed): her
    // post hangi alt/hastalık grubundan geldiğini göstermeli. Post.subGroup
    // lazy proxy'sinden .getName() okumak her post için ayrı bir SELECT
    // tetikler (id okumak proxy üzerinden bedavayken isim okumak proxy'yi
    // initialize eder - N+1). Bunun yerine sayfadaki DİSTİNCT subGroupId'ler
    // için TEK sorguda isim + üst grup ismini birlikte çekiyoruz (reactions/
    // saves/attachments ile aynı toplu enrichment deseni, bkz.
    // PostResponseAssembler.enrich).
    @Query("SELECT sg.id AS subGroupId, sg.name AS subGroupName, " +
            "dg.id AS diseaseGroupId, dg.name AS diseaseGroupName " +
            "FROM SubGroup sg JOIN sg.diseaseGroup dg WHERE sg.id IN :ids")
    List<SubGroupNameProjection> findNameProjectionsByIdIn(@Param("ids") Collection<Long> ids);

    interface SubGroupNameProjection {
        Long getSubGroupId();
        String getSubGroupName();
        Long getDiseaseGroupId();
        String getDiseaseGroupName();
    }
}
