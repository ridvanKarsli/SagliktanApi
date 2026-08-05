package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.BlockedUser;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.BlockedUserRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BadRequestException("Kendinizi engelleyemezsiniz");
        }
        if (blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return; // zaten engelli, idempotent
        }
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        blockedUserRepository.save(BlockedUser.builder().blocker(blocker).blocked(blocked).build());
    }

    @Override
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public List<BlockedUser> listBlocked(Long blockerId) {
        return blockedUserRepository.findByBlockerId(blockerId);
    }

    @Override
    public void assertNotBlocked(Long userAId, Long userBId) {
        if (isBlockedEitherDirection(userAId, userBId)) {
            throw new ForbiddenException("Bu kullanıcıyla mesajlaşamazsınız");
        }
    }

    @Override
    public boolean isBlockedEitherDirection(Long userAId, Long userBId) {
        return blockedUserRepository.existsByBlockerIdAndBlockedId(userAId, userBId)
                || blockedUserRepository.existsByBlockerIdAndBlockedId(userBId, userAId);
    }
}
