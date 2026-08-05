package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<BlockedUser> findByBlockerId(Long blockerId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
