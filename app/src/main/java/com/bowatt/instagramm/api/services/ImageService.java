package com.bowatt.instagramm.api.services;

import com.bowatt.instagramm.api.config.StorageProperties;
import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import com.bowatt.instagramm.api.repositories.ImageRepository;
import com.bowatt.instagramm.api.repositories.TagRepository;
import com.bowatt.instagramm.api.web.ImageContentType;
import com.bowatt.instagramm.api.web.ImageNotFoundException;
import com.bowatt.instagramm.api.web.ImageUploadException;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final ImageEventPublisher imageEventPublisher;
    private final Path uploadDirectory;

    @Value("${server.base-url}")
    private String baseUrl;

    @Value("${server.port}")
    private int port;

    @Value("${app.storage.upload-path}")
    private String uploadPath;

    private static final Logger logger = Logger.getLogger(ImageService.class.getName());

    public ImageService(
            ImageRepository imageRepository,
            TagRepository tagRepository,
            ImageEventPublisher imageEventPublisher,
            StorageProperties storageProperties) {
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.imageEventPublisher = imageEventPublisher;
        this.uploadDirectory = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public String getFullBaseUrl() {
        return baseUrl + ":" + port;
    }

    @Transactional
    public ImageResponse upload(MultipartFile file, String title, Set<String> tags) {
        if (file == null || file.isEmpty() ) {
            throw new ImageUploadException("Image file is required");
        }

        if (file.getContentType() == null){
            throw new ImageUploadException("Image content type is required");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new ImageUploadException("Image original filename is required");
        }

        if (title == null || title.isEmpty()) {
            throw new ImageUploadException("Title is required");
        }

        ImageContentType imageContentType =
                ImageContentType.fromMediaType(file.getContentType())
                        .orElseThrow(
                                () ->
                                        new ImageUploadException(
                                                "Unsupported image type. Allowed: "
                                                        + ImageContentType.allowedLabels()));

        String contentType = imageContentType.mediaType();
        String extension = imageContentType.extension();
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

        ImageResponse response = toResponse(saved);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> imageEventPublisher.publishImageCreated());
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Failed to publish image created event", e);
        }
        return response;
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
                this.getFullBaseUrl() + uploadPath + "/" + image.getStoredFilename(),
                image.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long id) {
        return imageRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ImageNotFoundException("Image not found with id: " + id));
    }
}
