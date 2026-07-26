package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.request.SubGroupRequest;
import com.ridvankarsli.sagliktanapi.dto.response.SubGroupResponse;
import com.ridvankarsli.sagliktanapi.service.SubGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubGroupController {

    private final SubGroupService subGroupService;

    @GetMapping("/api/disease-groups/{diseaseGroupId}/sub-groups")
    public List<SubGroupResponse> listByDiseaseGroup(@PathVariable Long diseaseGroupId) {
        return subGroupService.listByDiseaseGroup(diseaseGroupId).stream().map(SubGroupResponse::from).toList();
    }

    @PostMapping("/api/disease-groups/{diseaseGroupId}/sub-groups")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SubGroupResponse create(@PathVariable Long diseaseGroupId, @Valid @RequestBody SubGroupRequest request) {
        return SubGroupResponse.from(subGroupService.create(diseaseGroupId, request.name(), request.description()));
    }

    @GetMapping("/api/sub-groups/{id}")
    public SubGroupResponse getById(@PathVariable Long id) {
        return SubGroupResponse.from(subGroupService.getById(id));
    }

    @PutMapping("/api/sub-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubGroupResponse update(@PathVariable Long id, @Valid @RequestBody SubGroupRequest request) {
        return SubGroupResponse.from(subGroupService.update(id, request.name(), request.description()));
    }

    @DeleteMapping("/api/sub-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        subGroupService.delete(id);
    }
}
