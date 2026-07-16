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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;  
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.apache.tika.Tika;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;
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
            TagService tagService,
            ImageEventPublisher imageEventPublisher,
            StorageProperties storageProperties) {
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.imageEventPublisher = imageEventPublisher;
        this.uploadDirectory = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public String getFullBaseUrl() {
        if (baseUrl !=null && baseUrl.startsWith("https")) {
            return baseUrl;
        }
        return baseUrl + ":" + port;
    }

    @CacheEvict(value = "images", allEntries = true)
    @Transactional
    public ImageResponse upload(MultipartFile file, @Nullable String title, @Nullable Set<String> tags) {
        if (file == null || file.isEmpty() ) {
            throw new ImageUploadException("Image file is required");
        }

        if (file.getContentType() == null){
            throw new ImageUploadException("Image content type is required");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new ImageUploadException("Image original filename is required");
        }

        if (file.getOriginalFilename().length() > 255) {
            throw new ImageUploadException("Original filename must be at most 255 characters");
        }

        if (tags != null && tags.size() > 10) {
            throw new ImageUploadException("Tags must be at most 10");
        }

        if (title != null && title.length() > 255) {
            throw new ImageUploadException("Title must be at most 255 characters");
        }

        ImageContentType imageContentType = verifyImageContentType(file);

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

        TagResolution tagResolution = resolveTags(tags);
        if (tagResolution.newTagCreated()) {
            tagService.evictListCache();
        }

        Image saved =
                imageRepository.save(
                        new Image(
                                file.getOriginalFilename(),
                                storedFilename,
                                contentType,
                                file.getSize(),
                                title,
                                tagResolution.tags()));

        ImageResponse response = toResponse(saved);

        publishImageCreatedEvent();
        return response;
    }

    public ImageContentType verifyImageContentType(MultipartFile file) {
        String mimeType;
        ImageContentType imageContentType;
        try (InputStream inputStream = file.getInputStream()){
            Tika tika = new Tika();
            mimeType = tika.detect(inputStream, file.getOriginalFilename());

            imageContentType = ImageContentType.fromMediaType(mimeType)
                .orElseThrow(
                    () -> new ImageUploadException("Unsupported image type. Allowed: "+ ImageContentType.allowedLabels()));      

        } catch (IOException e) {
            throw new ImageUploadException("Wrong content Type");
        }
        
        return imageContentType;
    }

    @Async
    public void publishImageCreatedEvent() {
        logger.info("Publishing image created event");
        imageEventPublisher.publishImageCreated();
    }

    @Cacheable(
        value = "images",
        key = "T(java.lang.String).format('%s-%s-%s-%s', " +
              "#pageable.pageNumber, " +
              "#pageable.pageSize, " +
              "#pageable.sort.toString(), " +
              "(#tags == null ? 'no-tags' : #tags.stream().sorted().toList()))"
    )
    @Transactional(readOnly = true)
    public Page list(Pageable pageable, @Nullable Set<String> tags) {
        var normalizedTags = normalizeTagNames(tags);
        var page =
                normalizedTags.isEmpty()
                        ? imageRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                        : imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(normalizedTags, normalizedTags.size(), pageable);
        return new Page(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Set<String> normalizeTagNames(@Nullable Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        return tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TagResolution resolveTags(@Nullable Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new TagResolution(Set.of(), false);
        }

        boolean newTagCreated = false;
        Set<Tag> tags = new LinkedHashSet<>();
        for (String rawName : tagNames) {
            String name = rawName.trim();
            if (name.isEmpty()) {
                continue;
            }
            Optional<Tag> existing = tagRepository.findByName(name);
            if (existing.isPresent()) {
                tags.add(existing.get());
            } else {
                tags.add(tagRepository.save(new Tag(name)));
                newTagCreated = true;
            }
        }
        return new TagResolution(tags, newTagCreated);
    }

    private record TagResolution(Set<Tag> tags, boolean newTagCreated) {}

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

    @Cacheable(
        value = "image",
        key = "#id"
    )
    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long id) {
        return imageRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ImageNotFoundException("Image not found with id: " + id));
    }
}
