package com.bowatt.instagramm.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bowatt.instagramm.api.config.StorageProperties;
import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import com.bowatt.instagramm.api.repositories.ImageRepository;
import com.bowatt.instagramm.api.repositories.TagRepository;
import com.bowatt.instagramm.api.web.ImageContentType;
import com.bowatt.instagramm.api.web.ImageUploadException;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final byte[] JPEG_BYTES =
            Base64.getDecoder()
                    .decode(
                            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");
    private static final byte[] PNG_BYTES =
            Base64.getDecoder()
                    .decode(
                            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    private static final byte[] GIF_BYTES =
            Base64.getDecoder()
                    .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @TempDir Path tempDir;

    @Mock private ImageRepository imageRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TagService tagService;
    @Mock private ImageEventPublisher imageEventPublisher;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService =
                spy(
                        new ImageService(
                                imageRepository,
                                tagRepository,
                                tagService,
                                imageEventPublisher,
                                new StorageProperties(tempDir.toString())));
    }

    @Test
    void uploadStoresFileAndPersistsMetadata() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", JPEG_BYTES);

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
        verify(imageService).publishImageCreatedEvent();
        verify(tagService).evictListCache();
    }

    @Test
    void uploadDoesNotEvictTagsCacheWhenAllTagsAlreadyExist() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        when(tagRepository.findByName("beach")).thenReturn(Optional.of(new Tag("beach")));
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        imageService.upload(file, "title", Set.of("beach"));

        verify(tagService, never()).evictListCache();
    }

    @Test
    void uploadAcceptsNullTitle() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.upload(file, null, Set.of());

        assertNull(response.title());

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertNull(captor.getValue().getTitle());
        assertTrue(Files.exists(tempDir.resolve(captor.getValue().getStoredFilename())));
        verify(imageService).publishImageCreatedEvent();
    }

    @Test
    void uploadAcceptsNullTags() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.upload(file, "sunset", null);

        assertEquals(Set.of(), response.tags());

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertTrue(captor.getValue().getTags().isEmpty());
        assertTrue(Files.exists(tempDir.resolve(captor.getValue().getStoredFilename())));
        verify(imageService).publishImageCreatedEvent();
    }

    @Test
    void publishImageCreatedEvent_publishesToPublisher() {
        imageService.publishImageCreatedEvent();

        verify(imageEventPublisher).publishImageCreated();
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

        when(imageRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 20)))
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

    @Test
    void verifyImageContentType_acceptsJpeg() {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        ImageContentType result = imageService.verifyImageContentType(file);

        assertEquals(ImageContentType.JPEG, result);
        assertEquals("image/jpeg", result.mediaType());
        assertEquals(".jpg", result.extension());
    }

    @Test
    void verifyImageContentType_acceptsPng() {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        ImageContentType result = imageService.verifyImageContentType(file);

        assertEquals(ImageContentType.PNG, result);
        assertEquals("image/png", result.mediaType());
        assertEquals(".png", result.extension());
    }

    @Test
    void verifyImageContentType_acceptsGif() {
        MockMultipartFile file =
                new MockMultipartFile("file", "animation.gif", "image/gif", GIF_BYTES);

        ImageContentType result = imageService.verifyImageContentType(file);

        assertEquals(ImageContentType.GIF, result);
        assertEquals("image/gif", result.mediaType());
        assertEquals(".gif", result.extension());
    }

    @Test
    void verifyImageContentType_rejectsUnsupportedType() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "notes.txt",
                        "text/plain",
                        "plain text".getBytes(StandardCharsets.UTF_8));

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.verifyImageContentType(file));

        assertEquals(
                "Unsupported image type. Allowed: " + ImageContentType.allowedLabels(),
                ex.getMessage());
    }

    @Test
    void verifyImageContentType_wrapsIOExceptionFromInputStream() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("broken stream"));

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> imageService.verifyImageContentType(file));

        assertEquals("Wrong content Type", ex.getMessage());
    }

    @Test
    void getFullBaseUrl_appendsPortForHttpBaseUrl() {
        ReflectionTestUtils.setField(imageService, "baseUrl", "http://localhost");
        ReflectionTestUtils.setField(imageService, "port", 8080);

        assertEquals("http://localhost:8080", imageService.getFullBaseUrl());
    }

    @Test
    void getFullBaseUrl_returnsHttpsBaseUrlWithoutPort() {
        ReflectionTestUtils.setField(imageService, "baseUrl", "https://insta.api.usomi.de");
        ReflectionTestUtils.setField(imageService, "port", 443);

        assertEquals("https://insta.api.usomi.de", imageService.getFullBaseUrl());
    }
}
