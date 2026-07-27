package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    // parentCommentId null ise üst-seviye yorum, dolu ise bir yoruma (ya da
    // bir yanıta) yanıt oluşturulur. Derinlik sınırsızdır.
    Comment create(Long postId, Long userId, String content, Long parentCommentId);

    // Sadece üst-seviye yorumları sayfalar; tüm alt yanıtlar ayrı
    // getirilir (bkz. listDescendants) ve controller katmanında ağaca
    // dönüştürülür.
    Page<Comment> listByPost(Long postId, Pageable pageable);

    // Bir postun tüm alt-seviye yanıtlarını (her derinlikten) tek seferde
    // döner - N+1 sorgu / recursive DB çağrısı olmadan bellekte ağaç
    // kurmak için kullanılır.
    List<Comment> listDescendants(Long postId);

    // Gelişmiş arama: yorum içeriğinde tam metin arama.
    Page<Comment> search(String query, Pageable pageable);

    Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content);

    // Soft delete: yorum silinmiş olarak işaretlenir, satır silinmez ki
    // altındaki yanıt zinciri kopmasın.
    void delete(Long commentId, Long requesterId, boolean requesterIsAdmin);
}
