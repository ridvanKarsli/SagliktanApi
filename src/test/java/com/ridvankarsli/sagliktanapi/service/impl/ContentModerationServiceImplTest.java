package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Rapor: ContentModerationService'in iki temel davranışını doğrular -
// küfür/spam ENGELLER, kriz sinyali sadece İŞARETLER, asla engellemez
// (bkz. ContentModerationService javadoc'u).
class ContentModerationServiceImplTest {

    private final ContentModerationService service = new ContentModerationServiceImpl();

    @Test
    void moderate_returnsClean_forOrdinaryText() {
        ContentModerationService.ModerationResult result = service.moderate("Bugün doktoruma gittim, iyi geçti.");

        assertFalse(result.blocked());
        assertFalse(result.sensitive());
    }

    @Test
    void moderate_returnsClean_forNullOrBlankText() {
        assertFalse(service.moderate(null).blocked());
        assertFalse(service.moderate("   ").blocked());
    }

    // Tam kelime eşleşmesi: küfür listesindeki bir kelimenin İÇİNDE geçtiği
    // masum bir kelime yanlışlıkla engellenmemeli.
    @Test
    void moderate_doesNotBlock_whenBannedWordIsOnlyASubstringOfALegitWord() {
        // "sik" listede var ama "sikke" (madeni para) tamamen farklı bir kelime.
        ContentModerationService.ModerationResult result = service.moderate("Elimde eski bir sikke buldum.");

        assertFalse(result.blocked());
    }

    @Test
    void moderate_blocksAndDoesNotFlagSensitive_whenTextContainsBannedWord() {
        // "salak" listede TAM KELİME olarak yer alıyor - Türkçe çekim eki
        // eklenmiş bir hâli ("salaksın" gibi) kasıtlı olarak KULLANILMADI,
        // çünkü basit tam-kelime eşleşmesi böyle bir çekimi yakalamaz (bkz.
        // ContentModerationServiceImpl javadoc'undaki bilinen sınırlama).
        ContentModerationService.ModerationResult result = service.moderate("Bu gerçekten salak bir yorum.");

        assertTrue(result.blocked());
        assertFalse(result.sensitive());
    }

    @Test
    void moderate_blocks_whenTextContainsTooManyUrls() {
        String spammy = "Bak şuraya http://a.com http://b.com http://c.com kesin kazanırsın";

        assertTrue(service.moderate(spammy).blocked());
    }

    @Test
    void moderate_blocks_whenTextContainsLongRepeatedCharacterRun() {
        assertTrue(service.moderate("harikaaaaaaaaaa").blocked());
    }

    // KRİTİK: kriz sinyali ASLA engellenmemeli - sadece işaretlenmeli.
    @Test
    void moderate_neverBlocks_whenTextContainsCrisisSignal_onlyFlagsAsSensitive() {
        ContentModerationService.ModerationResult result =
                service.moderate("Son zamanlarda çok zorlanıyorum, artık dayanamıyorum ve kendime zarar vermeyi düşünüyorum.");

        assertFalse(result.blocked());
        assertTrue(result.sensitive());
    }
}
