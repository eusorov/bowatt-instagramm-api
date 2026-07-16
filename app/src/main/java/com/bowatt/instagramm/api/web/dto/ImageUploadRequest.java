package com.bowatt.instagramm.api.web.dto;

import com.bowatt.instagramm.api.web.validation.ValidImageFile;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

public class ImageUploadRequest {

    @ValidImageFile
    private @Nullable MultipartFile file;

    @Size(max = 255, message = "Title must be at most 255 characters")
    private @Nullable String title;

    @Size(max = 10, message = "Tags must be at most 10")
    private @Nullable Set<String> tags;

    public @Nullable MultipartFile getFile() {
        return file;
    }

    public void setFile(@Nullable MultipartFile file) {
        this.file = file;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable Set<String> getTags() {
        return tags;
    }

    public void setTags(@Nullable Set<String> tags) {
        this.tags = tags;
    }
}
