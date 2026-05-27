package com.bowatt.instagramm.api.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

public record ImageResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        @Nullable String title,
        @Nullable Set<String> tags,
        String url,
        Instant createdAt) {

    public record Page(
            List<ImageResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {}
}
