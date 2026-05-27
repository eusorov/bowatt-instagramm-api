package com.bowatt.instagramm.api.services;

import com.bowatt.instagramm.api.config.StorageProperties;
import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import com.bowatt.instagramm.api.repositories.ImageRepository;
import com.bowatt.instagramm.api.repositories.TagRepository;
import com.bowatt.instagramm.api.web.ImageNotFoundException;
import com.bowatt.instagramm.api.web.ImageUploadException;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final Path uploadDirectory;

    @Value("${server.base-url}")
    private String baseUrl;

    @Value("${server.port}")
    private int port;

    @Value("${app.storage.upload-path}")
    private String uploadPath;

    public ImageService(
            ImageRepository imageRepository,
            TagRepository tagRepository,
            StorageProperties storageProperties) {
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.uploadDirectory = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public String getFullBaseUrl() {
        return baseUrl + ":" + port;
    }

    @Transactional
    public ImageResponse upload(MultipartFile file, String title, Set<String> tags) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageUploadException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }

        String extension = extensionForContentType(contentType);
        UUID uuid = UUID.randomUUID();
        String storedFilename = uuid.toString() + extension;
        Path destination = uploadDirectory.resolve(storedFilename);

        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(destination);
        } catch (IOException ex) {
            throw new ImageUploadException("Failed to store uploaded image", ex);
        }

        Instant createdAt = Instant.now();
        Image saved =
                imageRepository.save(
                        new Image(
                                file.getOriginalFilename(),
                                storedFilename,
                                contentType,
                                file.getSize(),
                                title,
                                resolveTags(tags),
                                createdAt));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page list(Pageable pageable) {
        return list(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page list(Pageable pageable, Set<String> tags) {
        var normalizedTags = normalizeTagNames(tags);
        var page =
                normalizedTags.isEmpty()
                        ? imageRepository.findAllByOrderByCreatedAtDesc(pageable)
                        : imageRepository.findByAllTagNames(
                                normalizedTags, normalizedTags.size(), pageable);
        return new Page(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Set<String> normalizeTagNames(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        return tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        return tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(
                        name ->
                                tagRepository
                                        .findByName(name)
                                        .orElseGet(() -> tagRepository.save(new Tag(name))))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ImageResponse toResponse(Image image) {
        Set<String> tagNames =
                image.getTags().stream().map(Tag::getName).collect(Collectors.toCollection(LinkedHashSet::new));

        return new ImageResponse(
                image.getId(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getSizeBytes(),
                image.getTitle(),
                tagNames,
                // get base URL from application.yml
                this.getFullBaseUrl() + uploadPath + "/" + image.getStoredFilename(),
                image.getCreatedAt());
    }

    private static String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new ImageUploadException("Unsupported image type");
        };
    }

    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long id) {
        return imageRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ImageNotFoundException("Image not found with id: " + id));
    }
}
