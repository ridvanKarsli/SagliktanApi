package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.AdminReportActionRequest;
import com.ridvankarsli.sagliktanapi.dto.request.AdminUserUpdateRequest;
import com.ridvankarsli.sagliktanapi.dto.response.AdminReportResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminStatsResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminUserResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Tüm admin paneli uçları burada toplanıyor. Sınıf seviyesindeki
// @PreAuthorize, SecurityConfig'deki genel authenticated() kuralının
// üzerine ikinci bir savunma katmanı ekliyor - bir route eşleşme hatası
// olsa bile method security bağımsız olarak reddeder.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Role role,
            Pageable pageable
    ) {
        return PageResponse.from(adminService.listUsers(q, active, role, pageable).map(AdminUserResponse::from));
    }

    @PutMapping("/users/{id}")
    public AdminUserResponse updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return AdminUserResponse.from(adminService.updateUser(
                id, principal.getId(), request.firstName(), request.lastName(), request.bio(),
                request.role(), request.active()
        ));
    }

    @GetMapping("/reports")
    public PageResponse<AdminReportResponse> listReports(
            @RequestParam(required = false) ReportStatus status, Pageable pageable
    ) {
        return PageResponse.from(adminService.listReports(status, pageable).map(AdminReportResponse::from));
    }

    @PutMapping("/reports/{id}")
    public MessageResponse resolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminReportActionRequest request
    ) {
        adminService.resolveReport(id, principal.getId(), request.status());
        return new MessageResponse("Şikayet güncellendi");
    }
}
