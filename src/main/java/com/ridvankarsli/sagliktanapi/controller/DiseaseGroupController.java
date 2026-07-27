package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.dto.request.DiseaseGroupRequest;
import com.ridvankarsli.sagliktanapi.dto.response.DiseaseGroupResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserSearchResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.DiseaseGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/disease-groups")
@RequiredArgsConstructor
public class DiseaseGroupController {

    private final DiseaseGroupService diseaseGroupService;

    @GetMapping
    public List<DiseaseGroupResponse> listAll() {
        return diseaseGroupService.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DiseaseGroupResponse getById(@PathVariable Long id) {
        return toResponse(diseaseGroupService.getById(id));
    }

    // Gruba kayıtlı üyelerin herkese açık listesi - e-posta gibi hassas
    // alanlar döndürülmez, bkz. UserSearchResponse.
    @GetMapping("/{id}/members")
    public List<UserSearchResponse> listMembers(@PathVariable Long id) {
        return diseaseGroupService.listMembers(id).stream().map(UserSearchResponse::from).toList();
    }

    private DiseaseGroupResponse toResponse(DiseaseGroup group) {
        return DiseaseGroupResponse.from(group, diseaseGroupService.countMembers(group.getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DiseaseGroupResponse create(@Valid @RequestBody DiseaseGroupRequest request) {
        return DiseaseGroupResponse.from(diseaseGroupService.create(request.name(), request.description()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DiseaseGroupResponse update(@PathVariable Long id, @Valid @RequestBody DiseaseGroupRequest request) {
        return DiseaseGroupResponse.from(diseaseGroupService.update(id, request.name(), request.description()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        diseaseGroupService.delete(id);
    }

    @PostMapping("/{id}/join")
    public void join(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        diseaseGroupService.join(principal.getId(), id);
    }

    @DeleteMapping("/{id}/leave")
    public void leave(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        diseaseGroupService.leave(principal.getId(), id);
    }
}
