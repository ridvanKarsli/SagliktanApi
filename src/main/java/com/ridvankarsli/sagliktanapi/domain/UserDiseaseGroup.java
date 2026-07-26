package com.ridvankarsli.sagliktanapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Kullanıcı - hastalık grubu üyeliği (N:N ilişkinin ara varlığı, joined_at
// gibi ek kolon taşıdığı için düz @ManyToMany yerine ayrı entity olarak modellendi).
@Entity
@Table(name = "user_disease_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "diseaseGroup"})
public class UserDiseaseGroup {

    @EmbeddedId
    private UserDiseaseGroupId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("diseaseGroupId")
    @JoinColumn(name = "disease_group_id", nullable = false)
    private DiseaseGroup diseaseGroup;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;
}
