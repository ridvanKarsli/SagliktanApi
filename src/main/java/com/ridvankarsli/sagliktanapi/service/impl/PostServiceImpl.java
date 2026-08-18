package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import com.ridvankarsli.sagliktanapi.service.OwnershipGuard;
import com.ridvankarsli.sagliktanapi.service.PostAttachmentService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.service.PostSortOption;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final SubGroupRepository subGroupRepository;
    private final UserRepository userRepository;
    private final UserDiseaseGroupRepository userDiseaseGroupRepository;
    // Faz 2 adım 4: post + fotoğrafları tek bir aggregate/kullanım senaryosu
    // olarak ele alınıyor (Reaction/SavedPost'un aksine - onlar post'a
    // sadece id ile referans veren bağımsız aggregate'ler, bu yüzden
    // controller katmanında ayrı ayrı kompoze ediliyorlar). Attachment
    // doğrulaması burada, aynı @Transactional sınırı içinde yapılıyor ki
    // geçersiz bir fotoğraf referansı TÜM post oluşturmayı geri alsın.
    private final PostAttachmentService postAttachmentService;
    private final OwnershipGuard ownershipGuard;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional
    public Post create(Long subGroupId, Long userId, String title, String content, List<String> attachmentKeys) {
        SubGroup subGroup = subGroupRepository.findById(subGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Alt grup bulunamadı"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        assertMemberOfGroup(userId, subGroup.getDiseaseGroup().getId());
        boolean sensitive = moderateOrThrow(title, content);

        Post post = Post.builder()
                .subGroup(subGroup)
                .user(user)
                .title(title)
                .content(content)
                .flaggedSensitive(sensitive)
                .build();

        post = postRepository.save(post);
        postAttachmentService.attach(post, attachmentKeys);
        return post;
    }

    @Override
    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gönderi bulunamadı"));
    }

    @Override
    public Page<Post> listBySubGroup(Long subGroupId, PostSortOption sort, Pageable pageable) {
        // KÖK NEDEN (canlıda görüldü): Controller'daki ?sort= query
        // parametresi, Spring'in Pageable argument resolver'ının KENDİ
        // page/size/sort binding'i ile aynı isimde - "sort=recent" gelince
        // Spring bunu Pageable.getSort() içine "recent alanına göre sırala"
        // olarak dolduruyor, bu da derived query'de Post'ta "recent" diye
        // bir alan aranıp PropertyReferenceException'a yol açıyor.
        // Sıralamayı tamamen PostSortOption üzerinden biz yönettiğimiz için
        // Pageable'dan gelen Sort'u HER iki dalda da (sadece popular'da
        // değil) temizlemek gerekiyor.
        Pageable safePageable = SearchQueryUtil.stripSort(pageable);
        if (sort == PostSortOption.POPULAR) {
            return postRepository.findBySubGroupIdOrderByPopularityDesc(subGroupId, safePageable);
        }
        return postRepository.findBySubGroupIdOrderByCreatedAtDesc(subGroupId, safePageable);
    }

    @Override
    public Page<Post> listByUser(Long userId, Pageable pageable) {
        // Faz6: sabitlenmiş gönderi (varsa) her zaman en başta - bkz.
        // PostRepository.findByUserIdOrderByPinnedDescCreatedAtDesc javadoc'u.
        return postRepository.findByUserIdOrderByPinnedDescCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<Post> getFeedForUser(Long userId, PostSortOption sort, Pageable pageable) {
        // findFeedForUser/findFeedForUserOrderByPopularityDesc kendi ORDER
        // BY'ını taşıyor - listBySubGroup/search ile aynı gerekçeyle (bkz.
        // SearchQueryUtil.stripSort javadoc) çift ORDER BY Postgres syntax
        // hatasını önlemek için Pageable'dan Sort'u temizliyoruz.
        Pageable safePageable = SearchQueryUtil.stripSort(pageable);
        if (sort == PostSortOption.POPULAR) {
            return postRepository.findFeedForUserOrderByPopularityDesc(userId, safePageable);
        }
        return postRepository.findFeedForUser(userId, safePageable);
    }

    @Override
    public Page<Post> search(String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return postRepository.search(query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    public Page<Post> searchBySubGroup(Long subGroupId, String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return postRepository.searchBySubGroup(subGroupId, query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    public Page<Post> listWithAttachments(Pageable pageable) {
        return postRepository.findWithAttachments(SearchQueryUtil.stripSort(pageable));
    }

    @Override
    public Page<Post> searchWithAttachments(String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return postRepository.searchWithAttachments(query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    @Transactional
    public Post update(Long postId, Long requesterId, boolean requesterIsAdmin, String title, String content) {
        Post post = getById(postId);
        ownershipGuard.assertOwnerOrAdmin(post.getUser().getId(), requesterId, requesterIsAdmin);
        boolean sensitive = moderateOrThrow(title, content);

        post.setTitle(title);
        post.setContent(content);
        post.setFlaggedSensitive(sensitive);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void delete(Long postId, Long requesterId, boolean requesterIsAdmin) {
        Post post = getById(postId);
        ownershipGuard.assertOwnerOrAdmin(post.getUser().getId(), requesterId, requesterIsAdmin);
        // post_attachments.post_id zaten ON DELETE CASCADE (bkz. V14
        // migration), ama R2'deki gerçek dosyalar DB cascade'inin
        // kapsamı dışında - onları açıkça silmezsek sonsuza kadar orphan
        // kalırlar. Bu yüzden post satırı silinmeden ÖNCE, storage
        // key'leri hâlâ okunabilirken temizleniyor.
        postAttachmentService.deleteAllForPost(postId);
        postRepository.delete(post);
    }

    // Faz6: sabitlenmiş gönderi - kasıtlı olarak OwnershipGuard.assertOwnerOrAdmin
    // KULLANILMIYOR (update/delete'in aksine). Bu bir moderasyon işlemi değil,
    // saf bir profil kişiselleştirmesi - bir admin'in başka bir kullanıcının
    // profilinde neyin öne çıkacağına karar vermesi anlamsız/istenmeyen bir
    // yetki genişlemesi olurdu, bu yüzden burada gerçek sahiplik dışında
    // istisna yok.
    @Override
    @Transactional
    public Post pin(Long postId, Long userId) {
        Post post = getById(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Sadece kendi gönderinizi sabitleyebilirsiniz");
        }
        if (post.isPinned()) {
            return post;
        }
        // Önce varsa mevcut sabitlenmiş postu kaldır - V20'deki PARTIAL unique
        // index (posts.user_id WHERE pinned) zaten bunu DB seviyesinde
        // zorunlu kılıyor, burada önceden temizlemezsek constraint violation
        // fırlardı.
        postRepository.findByUserIdAndPinnedTrue(userId).ifPresent(previous -> {
            previous.setPinned(false);
            postRepository.save(previous);
        });
        post.setPinned(true);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post unpin(Long postId, Long userId) {
        Post post = getById(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Sadece kendi gönderinizin sabitini kaldırabilirsiniz");
        }
        post.setPinned(false);
        return postRepository.save(post);
    }

    // Kullanıcı, gönderi paylaşacağı alt grubun bağlı olduğu hastalık grubuna
    // üye değilse işlem reddedilir - üye olmayanın gruba post atması engellenir.
    private void assertMemberOfGroup(Long userId, Long diseaseGroupId) {
        if (!userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(userId, diseaseGroupId)) {
            throw new ForbiddenException("Bu hastalık grubuna üye değilsiniz, gönderi paylaşamazsınız");
        }
    }

    // Rapor: başlık + içerik BİRLİKTE moderasyondan geçer - herhangi biri
    // küfür/spam içeriyorsa (blocked) BadRequestException fırlatılır;
    // herhangi biri kriz sinyali taşıyorsa (bloklamadan) true döner (bkz.
    // ContentModerationService javadoc'u).
    private boolean moderateOrThrow(String title, String content) {
        ContentModerationService.ModerationResult titleResult = contentModerationService.moderate(title);
        if (titleResult.blocked()) {
            throw new BadRequestException(titleResult.blockReason());
        }
        ContentModerationService.ModerationResult contentResult = contentModerationService.moderate(content);
        if (contentResult.blocked()) {
            throw new BadRequestException(contentResult.blockReason());
        }
        return titleResult.sensitive() || contentResult.sensitive();
    }
}
