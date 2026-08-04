package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostSortOptionTest {

    @Test
    void fromParam_returnsRecent_whenNullOrBlank() {
        assertEquals(PostSortOption.RECENT, PostSortOption.fromParam(null));
        assertEquals(PostSortOption.RECENT, PostSortOption.fromParam(""));
        assertEquals(PostSortOption.RECENT, PostSortOption.fromParam("   "));
    }

    @Test
    void fromParam_isCaseInsensitive() {
        assertEquals(PostSortOption.RECENT, PostSortOption.fromParam("recent"));
        assertEquals(PostSortOption.RECENT, PostSortOption.fromParam("RECENT"));
        assertEquals(PostSortOption.POPULAR, PostSortOption.fromParam("popular"));
        assertEquals(PostSortOption.POPULAR, PostSortOption.fromParam("Popular"));
    }

    @Test
    void fromParam_throwsBadRequest_whenValueUnknown() {
        assertThrows(BadRequestException.class, () -> PostSortOption.fromParam("trending"));
    }
}
