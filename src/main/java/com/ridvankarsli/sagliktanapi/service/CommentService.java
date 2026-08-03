package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;

public interface CommentService {

    // parentCommentId null ise üst-seviye yorum, dolu ise bir yoruma (ya da
    // bir yanıta) yanıt oluşturulur. Derinlik sınırsızdır.
    Comment create(Long postId, Long userId, String content, Long parentCommentId);

    // Sadece üst-seviye yorumları sayfalar.
    Page<Comment> listByPost(Long postId, Pageable pageable);

    // Bir yorumun DOĞRUDAN yanıtlarını sayfalı döner (bir alt seviye, tüm
    // derinlik değil) - thread-drill arayüzü zaten her seferinde tek bir
    // seviye gösterdiği için, önceden postun tüm yorum ağacını sınırsız tek
    // seferde çeken yaklaşım yerine talep üzerine, ölçeklenebilir şekilde.
    Page<Comment> listReplies(Long parentCommentId, Pageable pageable);

    // Birden çok yorumun DOĞRUDAN yanıt sayısını tek sorguda, toplu döner.
    Map<Long, Long> countReplies(Collection<Long> parentIds);

    // Gelişmiş arama: yorum içeriğinde tam metin arama.
    Page<Comment> search(String query, Pageable pageable);

    Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content);

    // Soft delete: yorum silinmiş olarak işaretlenir, satır silinmez ki
    // altındaki yanıt zinciri kopmasın.
    void delete(Long commentId, Long requesterId, boolean requesterIsAdmin);
}
