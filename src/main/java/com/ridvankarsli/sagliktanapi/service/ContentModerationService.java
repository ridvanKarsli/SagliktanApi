package com.ridvankarsli.sagliktanapi.service;

// Rapor: basit içerik moderasyonu. İki farklı sinyal BİLEREK ayrı tutuluyor:
//  - blocked=true  -> küfür/spam: gönderim REDDEDİLİR (BadRequestException,
//    bkz. PostServiceImpl/CommentServiceImpl.moderateOrThrow).
//  - sensitive=true -> kriz sinyali (intihar/kendine zarar verme vb.):
//    gönderim ASLA ENGELLENMEZ, sadece işaretlenir - PostResponse/
//    CommentResponse üzerinden frontend'e taşınıp destekleyici bir kaynak
//    bilgisi (182 ALO Yaşam Hattı) gösterilir. Zor bir deneyimini paylaşmaya
//    çalışan biri asla susturulmamalı.
public interface ContentModerationService {

    ModerationResult moderate(String text);

    record ModerationResult(boolean blocked, String blockReason, boolean sensitive) {
        public static final ModerationResult CLEAN = new ModerationResult(false, null, false);
    }
}
