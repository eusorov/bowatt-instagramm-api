package com.bowatt.instagramm.api.web.dto;

import com.bowatt.instagramm.api.web.validation.ValidImageFile;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(
        @ValidImageFile MultipartFile file,
        @Size(max = 255, message = "Title must be at most 255 characters") @Nullable String title,
        @Size(max = 10, message = "Tags must be at most 10") @Nullable Set<String> tags) {}
