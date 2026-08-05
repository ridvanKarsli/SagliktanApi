package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.BlockedUser;

import java.util.List;

// Faz 2 adım 6: tek yönlü engelleme (blocker, blocked'ı engeller). Kontrol
// her zaman iki yönde de yapılır (bkz. assertNotBlocked) - A, B'yi
// engellemişse B de A'ya mesaj/istek gönderemesin diye.
public interface BlockService {

    void block(Long blockerId, Long blockedId);

    void unblock(Long blockerId, Long blockedId);

    List<BlockedUser> listBlocked(Long blockerId);

    // userAId ile userBId arasında HERHANGİ bir yönde engel varsa
    // ForbiddenException fırlatır. MessageRequestService.send ve
    // MessageService.send tarafından çağrılır.
    void assertNotBlocked(Long userAId, Long userBId);
}
