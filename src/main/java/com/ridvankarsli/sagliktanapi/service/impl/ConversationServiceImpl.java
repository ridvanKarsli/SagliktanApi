package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.ConversationRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<Conversation> findExisting(Long user1Id, Long user2Id) {
        long lowId = Math.min(user1Id, user2Id);
        long highId = Math.max(user1Id, user2Id);
        return conversationRepository.findByUserOneIdAndUserTwoId(lowId, highId);
    }

    @Override
    @Transactional
    public Conversation getOrCreateBetween(Long user1Id, Long user2Id) {
        return findExisting(user1Id, user2Id).orElseGet(() -> {
            long lowId = Math.min(user1Id, user2Id);
            long highId = Math.max(user1Id, user2Id);
            User userOne = userRepository.findById(lowId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
            User userTwo = userRepository.findById(highId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
            return conversationRepository.save(
                    Conversation.builder().userOne(userOne).userTwo(userTwo).build());
        });
    }

    @Override
    public Conversation getById(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Konuşma bulunamadı"));
    }

    @Override
    public void assertParticipant(Conversation conversation, Long requesterId) {
        boolean participant = conversation.getUserOne().getId().equals(requesterId)
                || conversation.getUserTwo().getId().equals(requesterId);
        if (!participant) {
            throw new ForbiddenException("Bu konuşmanın tarafı değilsiniz");
        }
    }

    @Override
    public Page<Conversation> listForUser(Long userId, Pageable pageable) {
        return conversationRepository.findAllForUser(userId, pageable);
    }

    @Override
    public User otherParticipant(Conversation conversation, Long userId) {
        return conversation.getUserOne().getId().equals(userId)
                ? conversation.getUserTwo()
                : conversation.getUserOne();
    }
}
