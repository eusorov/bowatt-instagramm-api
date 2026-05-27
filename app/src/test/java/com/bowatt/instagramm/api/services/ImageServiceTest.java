package com.bowatt.instagramm.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bowatt.instagramm.api.config.StorageProperties;
import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import com.bowatt.instagramm.api.repositories.ImageRepository;
import com.bowatt.instagramm.api.repositories.TagRepository;
import com.bowatt.instagramm.api.web.ImageUploadException;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @TempDir Path tempDir;

    @Mock private ImageRepository imageRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TagService tagService;
    @Mock private ImageEventPublisher imageEventPublisher;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService =
                new ImageService(
                        imageRepository,
                        tagRepository,
                        tagService,
                        imageEventPublisher,
                        new StorageProperties(tempDir.toString()));
    }

    @Test
    void uploadStoresFileAndPersistsMetadata() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));

        when(tagRepository.findByName("beach")).thenReturn(Optional.empty());
        when(tagRepository.findByName("sunset")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response =
                imageService.upload(file, "beach day", Set.of("beach", "sunset"));

        assertEquals("photo.jpg", response.originalFilename());
        assertEquals("image/jpeg", response.contentType());
        assertEquals("beach day", response.title());
        assertEquals(Set.of("beach", "sunset"), response.tags());

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        Image saved = captor.getValue();
        assertEquals(saved.getStoredFilename().endsWith(".jpg"), true);
        assertEquals("photo.jpg", saved.getOriginalFilename());
        assertEquals("beach day", saved.getTitle());
        assertEquals(2, saved.getTags().size());
        assertTrue(Files.exists(tempDir.resolve(saved.getStoredFilename())));
        verify(imageEventPublisher).publishImageCreated();
        verify(tagService).evictListCache();
    }

    @Test
    void uploadDoesNotEvictTagsCacheWhenAllTagsAlreadyExist() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));

        when(tagRepository.findByName("beach")).thenReturn(Optional.of(new Tag("beach")));
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        imageService.upload(file, "title", Set.of("beach"));

        verify(tagService, never()).evictListCache();
    }

    @Test
    void uploadAcceptsNullTitle() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));

        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.upload(file, null, Set.of());

        assertNull(response.title());

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertNull(captor.getValue().getTitle());
        assertTrue(Files.exists(tempDir.resolve(captor.getValue().getStoredFilename())));
        verify(imageEventPublisher).publishImageCreated();
    }

    @Test
    void uploadRejectsTitleLongerThan255Characters() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));
        String longTitle = "a".repeat(256);

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.upload(file, longTitle, Set.of()));

        assertEquals("Title must be at most 255 characters", ex.getMessage());
    }

    @Test
    void uploadAcceptsNullTags() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));

        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.upload(file, "sunset", null);

        assertEquals(Set.of(), response.tags());

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertTrue(captor.getValue().getTags().isEmpty());
        assertTrue(Files.exists(tempDir.resolve(captor.getValue().getStoredFilename())));
        verify(imageEventPublisher).publishImageCreated();
    }

    @Test
    void uploadRejectsOriginalFilenameLongerThan255Characters() {
        String longName = "a".repeat(256);
        MockMultipartFile file =
                new MockMultipartFile("file", longName, "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.upload(file, "title", Set.of()));

        assertEquals("Original filename must be at most 255 characters", ex.getMessage());
    }

    @Test
    void uploadRejectsFileLargerThanMaxSize() {
        byte[] oversizedContent = new byte[Image.MAX_FILENAME_SIZE + 1];
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", oversizedContent);

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.upload(file, "title", Set.of()));

        assertEquals(
                "Image size must be less than " + Image.MAX_FILENAME_SIZE + " bytes",
                ex.getMessage());
    }

    @Test
    void uploadRejectsMoreThanTenTags() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", "jpeg-content".getBytes(StandardCharsets.UTF_8));
        Set<String> elevenTags =
                IntStream.range(0, 11).mapToObj(i -> "tag" + i).collect(Collectors.toSet());

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.upload(file, "title", elevenTags));

        assertEquals("Tags must be at most 10", ex.getMessage());
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.upload(file, "title", Set.of()));

        assertEquals("Image file is required", ex.getMessage());
    }

    @Test
    void listReturnsPagedImagesWithTags() {
        Tag beachTag = new Tag("beach");
        Tag summerTag = new Tag("summer");
        Image image =
                new Image(
                        "photo.jpg",
                        "photo.jpg.jpg",
                        "image/jpeg",
                        128L,
                        "sunset",
                        new LinkedHashSet<>(Set.of(beachTag, summerTag))
                        );

        when(imageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(image), PageRequest.of(0, 20), 1));

        Page page = imageService.list(PageRequest.of(0, 20), Set.of());

        assertEquals(1, page.content().size());
        assertEquals("sunset", page.content().getFirst().title());
        assertEquals(Set.of("beach", "summer"), page.content().getFirst().tags());
        assertEquals(0, page.page());
        assertEquals(20, page.size());
        assertEquals(1, page.totalElements());
        assertEquals(1, page.totalPages());
    }
}
