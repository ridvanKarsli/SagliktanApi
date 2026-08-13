package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiseaseGroupRepository extends JpaRepository<DiseaseGroup, Long> {

    Optional<DiseaseGroup> findByName(String name);

    boolean existsByName(String name);

    // Gruplar sayfası arama kutusu (V17 migration). PostRepository.
    // SEARCH_MATCH_CONDITION/SEARCH_RELEVANCE_ORDER ile aynı desen: prefix
    // tsquery (:tsQuery) VE pg_trgm yazım hatası toleranslı benzerlik
    // (:rawQuery) OR ile birleşip GREATEST(...) skoruna göre sıralanıyor.
    // Grup sayısı admin tarafından oluşturulup sınırlı kaldığı için (bkz.
    // pagination denetimi) Page/countQuery'e gerek yok - PostRepository.
    // search()'ün aksine düz List dönüyor.
    @Query(
            value = "SELECT * FROM disease_groups g WHERE " +
                    "(g.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "OR word_similarity(:rawQuery, coalesce(g.name, '') || ' ' || coalesce(g.description, '')) > 0.3) " +
                    "ORDER BY GREATEST(" +
                    "    COALESCE(ts_rank(g.search_vector, to_tsquery('turkish', :tsQuery)), 0), " +
                    "    word_similarity(:rawQuery, coalesce(g.name, '') || ' ' || coalesce(g.description, ''))" +
                    ") DESC",
            nativeQuery = true)
    List<DiseaseGroup> search(@Param("rawQuery") String rawQuery, @Param("tsQuery") String tsQuery);
}
