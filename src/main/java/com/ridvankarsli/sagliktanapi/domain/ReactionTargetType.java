package com.ridvankarsli.sagliktanapi.domain;

// ContentReport'taki ReportTargetType ile aynı POST/COMMENT ayrımını temsil
// eder ama kasıtlı olarak ayrı bir enum: reaksiyon ve şikayet sistemleri
// birbirinden bağımsız evrilebilsin (ör. ileride reaksiyon farklı bir hedefe
// - profil, grup vb. - genişlerse şikayet sistemini etkilemesin).
public enum ReactionTargetType {
    POST,
    COMMENT
}
