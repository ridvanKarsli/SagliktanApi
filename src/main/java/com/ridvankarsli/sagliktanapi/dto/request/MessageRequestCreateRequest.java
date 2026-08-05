package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record MessageRequestCreateRequest(@NotNull Long recipientId) {
}
