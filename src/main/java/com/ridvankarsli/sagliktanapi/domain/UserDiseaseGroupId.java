package com.ridvankarsli.sagliktanapi.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// user_disease_groups tablosunun kompozit primary key'i (user_id, disease_group_id)
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserDiseaseGroupId implements Serializable {

    private Long userId;

    private Long diseaseGroupId;
}
