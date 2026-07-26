package com.ridvankarsli.sagliktanapi.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

// Spring Data Page'i doğrudan controller'dan döndürmek yerine (Jackson ile
// serileştirmesi Spring Boot 3+'ta önerilmiyor) sade bir DTO'ya sarıyoruz.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
