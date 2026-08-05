package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import org.springframework.stereotype.Component;

// Rapor 5.3: "kendi" kaydı üzerindeki işlemler için sahiplik kontrolü servis
// katmanında yapılmalı, sadece role bazlı kontrol yeterli değil. Bu kontrol
// PostServiceImpl ve CommentServiceImpl'de birebir aynı şekilde tekrarlanıyordu
// (bkz. clean-code audit) - artık ikisi de buraya delege ediyor.
@Component
public class OwnershipGuard {

    public void assertOwnerOrAdmin(Long ownerId, Long requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!ownerId.equals(requesterId)) {
            throw new ForbiddenException("Bu işlem için yetkiniz yok");
        }
    }
}
