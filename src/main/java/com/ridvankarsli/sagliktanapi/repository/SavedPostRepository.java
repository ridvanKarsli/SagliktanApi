package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    // Derived delete: eşleşen kayıt yoksa sessizce hiçbir şey yapmaz -
    // "kaydı olmayan bir postu kaydı kaldır" isteği idempotent kalır (bkz.
    // SavedPostServiceImpl), 404 fırlatmaya gerek yok.
    void deleteByUserIdAndPostId(Long userId, Long postId);

    // "Kaydedilenler" sekmesi: Post'u doğrudan JPQL join ile seçiyoruz -
    // SavedPost.post lazy olduğu için normal bir "Page<SavedPost> bul,
    // sonra .getPost() çağır" yaklaşımı repository sınırının dışında
    // LazyInitializationException'a yol açardı.
    @Query("select sp.post from SavedPost sp where sp.user.id = :userId order by sp.createdAt desc")
    Page<Post> findSavedPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Toplu "bu gönderiler kullanıcı tarafından kaydedilmiş mi" kontrolü -
    // ReactionRepository.countGrouped ile aynı toplu-sorgu mantığı, feed'deki
    // her post için ayrı ayrı sorgu atmamak için (bkz. PostController).
    @Query("select sp.post.id from SavedPost sp where sp.user.id = :userId and sp.post.id in :postIds")
    List<Long> findSavedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    // Postlarda gösterilen "N kişi kaydetti" sayısı için toplu kayıt sayısı -
    // yine ReactionRepository.countGrouped ile aynı desen.
    @Query("select sp.post.id as postId, count(sp) as count from SavedPost sp where sp.post.id in :postIds group by sp.post.id")
    List<SavedPostCountRow> countGrouped(@Param("postIds") Collection<Long> postIds);

    interface SavedPostCountRow {
        Long getPostId();
        long getCount();
    }
}
