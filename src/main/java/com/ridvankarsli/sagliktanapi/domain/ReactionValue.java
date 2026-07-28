package com.ridvankarsli.sagliktanapi.domain;

// Beğeni yerine bilinçli tercih: sağlık içerikli bir toplulukta "beğeni"
// anlamsız/rahatsız edici olabilir (ör. hastalıkla ilgili zor bir paylaşım).
// Bunun yerine içeriğin işe yarayıp yaramadığını ifade eden bir reaksiyon.
public enum ReactionValue {
    HELPFUL,
    NOT_HELPFUL
}
