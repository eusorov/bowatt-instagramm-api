package com.bowatt.instagramm.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false, length = 255)
    @Size(max = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    @Size(max = 255)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 127)
    @Size(max = 127)
    @Pattern(regexp = "^image/[a-zA-Z0-9]+$", message = "Invalid content type")
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    @Min(value = 0)
    private long sizeBytes;

    @Column(length = 255)
    @Size(max = 255)
    @Nullable private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
            name = "image_tags",
            joinColumns = @JoinColumn(name = "image_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Size(max = 10)
    private Set<Tag> tags = new HashSet<>();

    @SuppressWarnings("NullAway.Init")
    protected Image() {}

    @SuppressWarnings("NullAway.Init")
    public Image(
            String originalFilename,
            String storedFilename,
            String contentType,
            long sizeBytes,
            @Nullable String title,
            Set<Tag> tags) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.title = title;
        this.tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }
}
