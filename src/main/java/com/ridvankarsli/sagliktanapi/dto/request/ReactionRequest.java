package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(
        @NotNull(message = "Reaksiyon değeri zorunludur")
        ReactionValue value
) {
}
