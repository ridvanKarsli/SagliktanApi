package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroup;
import com.ridvankarsli.sagliktanapi.dto.response.UserDataExportResponse;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.ReactionRepository;
import com.ridvankarsli.sagliktanapi.repository.RefreshSessionRepository;
import com.ridvankarsli.sagliktanapi.repository.SavedPostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.UserService;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final UserDiseaseGroupRepository userDiseaseGroupRepository;
    private final SavedPostRepository savedPostRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName, String bio) {
        User user = getById(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBio(bio);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivate(Long userId) {
        User user = getById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public Page<User> search(String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return userRepository.search(query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    public ProfileStats getProfileStats(Long userId) {
        long postCount = postRepository.countByUserId(userId);
        long commentCount = commentRepository.countByUserIdAndDeletedFalse(userId);
        long likes = 0;
        long dislikes = 0;
        for (ReactionRepository.ReceivedReactionCountRow row : reactionRepository.countReceivedByUserId(userId)) {
            if (ReactionValue.HELPFUL.name().equals(row.getValue())) {
                likes = row.getCount();
            } else if (ReactionValue.NOT_HELPFUL.name().equals(row.getValue())) {
                dislikes = row.getCount();
            }
        }
        return new ProfileStats(postCount, commentCount, likes, dislikes);
    }

    @Override
    public UserDataExportResponse exportData(Long userId) {
        User user = getById(userId);

        List<Post> myPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged()).getContent();
        List<Comment> myComments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<String> groupNames = userDiseaseGroupRepository.findById_UserId(userId).stream()
                .map(udg -> udg.getDiseaseGroup().getName())
                .toList();
        List<Post> saved = savedPostRepository.findSavedPostsByUserId(userId, Pageable.unpaged()).getContent();

        return new UserDataExportResponse(
                new UserDataExportResponse.ProfileData(
                        user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                        user.getBio(), user.getCreatedAt()
                ),
                myPosts.stream()
                        .map(p -> new UserDataExportResponse.PostData(p.getId(), p.getTitle(), p.getContent(), p.getCreatedAt()))
                        .toList(),
                myComments.stream()
                        .map(c -> new UserDataExportResponse.CommentData(c.getId(), c.getContent(), c.isDeleted(), c.getCreatedAt()))
                        .toList(),
                groupNames,
                saved.stream()
                        .map(p -> new UserDataExportResponse.SavedPostRef(p.getId(), p.getTitle()))
                        .toList(),
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String rawPassword) {
        User user = getById(userId);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadRequestException("Şifre hatalı");
        }

        // Gönderi/yorum İÇERİĞİ silinmiyor (bkz. comments.deleted deseni,
        // V6 migration) - platformdaki tartışma zincirleri bozulmasın diye
        // satırlar kalır, sadece hesabın KİMLİK bilgisi anonimleştirilir.
        user.setFirstName("Silinmiş");
        user.setLastName("Kullanıcı");
        user.setEmail("silinmis-" + userId + "@sagliktan.local");
        user.setBio(null);
        // Şifre de rastgele bir değere çevriliyor - active=false zaten girişi
        // engelliyor ama bu, hesap ileride bir admin tarafından yanlışlıkla
        // tekrar aktifleştirilse bile eski şifreyle giriş yapılamamasını
        // garanti eden ek bir katman (defense in depth).
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);
        userRepository.save(user);

        // Grup üyelikleri ve kaydedilen gönderiler (bookmark) İÇERİK değil,
        // hesabın kişisel durumu - hesapla birlikte kaldırılır.
        for (UserDiseaseGroup membership : userDiseaseGroupRepository.findById_UserId(userId)) {
            userDiseaseGroupRepository.deleteById_UserIdAndId_DiseaseGroupId(
                    userId, membership.getDiseaseGroup().getId());
        }
        savedPostRepository.deleteByUserId(userId);
        // Hesap silindiğinde tüm cihazlardaki oturumlar da geçersiz olsun -
        // aksi halde silinmiş bir hesabın refresh token'ı (henüz süresi
        // dolmadıysa) teorik olarak yenilenebilirdi (bkz. RefreshSession).
        refreshSessionRepository.deleteByUserId(userId);
    }
}
