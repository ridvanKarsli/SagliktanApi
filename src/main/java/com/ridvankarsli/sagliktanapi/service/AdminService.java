package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.AdminStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminStatsResponse getStats();

    // q/active/role'den herhangi biri null ise o filtre uygulanmaz.
    Page<User> listUsers(String q, Boolean active, Role role, Pageable pageable);

    // requestingAdminId: admin kendi hesabını bu ekrandan pasifleştiremez /
    // yetkisini düşüremez (self-lockout koruması) - bkz. AdminServiceImpl.
    User updateUser(
            Long targetUserId, Long requestingAdminId,
            String firstName, String lastName, String bio, Role role, boolean active
    );

    Page<AdminReportItem> listReports(ReportStatus status, Pageable pageable);

    void resolveReport(Long reportId, Long adminId, ReportStatus newStatus);
}
