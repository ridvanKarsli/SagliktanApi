package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.request.UpdateProfileRequest;
import com.ridvankarsli.sagliktanapi.dto.response.DiseaseGroupResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserSearchResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.DiseaseGroupService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DiseaseGroupService diseaseGroupService;
    private final PostService postService;

    @GetMapping("/me")
    public UserResponse getProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return UserResponse.from(userService.getById(principal.getId()));
    }

    @PutMapping("/me")
    public UserResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return UserResponse.from(
                userService.updateProfile(principal.getId(), request.firstName(), request.lastName(), request.bio())
        );
    }

    @DeleteMapping("/me")
    public void deactivate(@AuthenticationPrincipal CustomUserDetails principal) {
        userService.deactivate(principal.getId());
    }

    @GetMapping("/me/disease-groups")
    public List<DiseaseGroupResponse> myDiseaseGroups(@AuthenticationPrincipal CustomUserDetails principal) {
        return diseaseGroupService.listUserGroups(principal.getId())
                .stream()
                .map(DiseaseGroupResponse::from)
                .toList();
    }

    @GetMapping("/me/posts")
    public PageResponse<PostResponse> myPosts(
            @AuthenticationPrincipal CustomUserDetails principal,
            Pageable pageable
    ) {
        return PageResponse.from(postService.listByUser(principal.getId(), pageable).map(PostResponse::from));
    }

    // Gelişmiş arama: ad/soyada göre kişi arama (bkz. V7 migration).
    // E-posta gibi hassas alanlar döndürülmez - bkz. UserSearchResponse.
    @GetMapping("/search")
    public PageResponse<UserSearchResponse> search(@RequestParam String q, Pageable pageable) {
        return PageResponse.from(userService.search(q, pageable).map(UserSearchResponse::from));
    }
}
