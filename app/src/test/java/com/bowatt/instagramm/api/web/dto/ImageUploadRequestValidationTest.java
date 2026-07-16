package com.bowatt.instagramm.api.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bowatt.instagramm.api.web.ImageContentType;
import com.bowatt.instagramm.api.web.ImageUploadException;
import com.bowatt.instagramm.api.web.validation.ImageFileValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageUploadRequestValidationTest {

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

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        var request = new ImageUploadRequest(validFile(), "sunset", Set.of("beach", "summer"));

        assertTrue(violations(request).isEmpty());
    }

    @Test
    void acceptsNullTitleAndTags() {
        var request = new ImageUploadRequest(validFile(), null, null);

        assertTrue(violations(request).isEmpty());
    }

    @Test
    void acceptsTitleWith255Characters() {
        var request = new ImageUploadRequest(validFile(), "a".repeat(255), null);

        assertTrue(violations(request).isEmpty());
    }

    @Test
    void acceptsExactlyTenTags() {
        Set<String> tenTags = IntStream.range(0, 10).mapToObj(i -> "tag" + i).collect(Collectors.toSet());
        var request = new ImageUploadRequest(validFile(), null, tenTags);

        assertTrue(violations(request).isEmpty());
    }

    @Test
    void acceptsJpegPngAndGifImageContent() {
        assertTrue(
                violations(
                                new ImageUploadRequest(
                                        new MockMultipartFile(
                                                "file", "photo.jpg", "image/jpeg", JPEG_BYTES),
                                        null,
                                        null))
                        .isEmpty());
        assertTrue(
                violations(
                                new ImageUploadRequest(
                                        new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES),
                                        null,
                                        null))
                        .isEmpty());
        assertTrue(
                violations(
                                new ImageUploadRequest(
                                        new MockMultipartFile(
                                                "file", "animation.gif", "image/gif", GIF_BYTES),
                                        null,
                                        null))
                        .isEmpty());
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullFile() {
        var request = new ImageUploadRequest(null, "title", Set.of());

        assertSingleViolation(request, "Image file is required");
    }

    @Test
    void rejectsEmptyFile() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image file is required");
    }

    @Test
    void rejectsMissingContentType() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile("file", "photo.jpg", null, JPEG_BYTES),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image content type is required");
    }

    @Test
    void rejectsNullOriginalFilename() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile("file", null, "image/jpeg", JPEG_BYTES),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image original filename is required");
    }

    @Test
    void rejectsEmptyOriginalFilename() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile("file", "", "image/jpeg", JPEG_BYTES),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image original filename is required");
    }

    @Test
    void rejectsOriginalFilenameLongerThan255Characters() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile("file", "a".repeat(256), "image/jpeg", JPEG_BYTES),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Original filename must be at most 255 characters");
    }

    @Test
    void rejectsTitleLongerThan255Characters() {
        var request = new ImageUploadRequest(validFile(), "a".repeat(256), Set.of());

        assertSingleViolation(request, "Title must be at most 255 characters");
    }

    @Test
    void rejectsMoreThanTenTags() {
        Set<String> elevenTags =
                IntStream.range(0, 11).mapToObj(i -> "tag" + i).collect(Collectors.toSet());
        var request = new ImageUploadRequest(validFile(), "title", elevenTags);

        assertSingleViolation(request, "Tags must be at most 10");
    }

    @Test
    void rejectsUnsupportedImageContentType() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile(
                                "file",
                                "notes.txt",
                                "text/plain",
                                "plain text".getBytes(StandardCharsets.UTF_8)),
                        "title",
                        Set.of());

        assertSingleViolation(
                request, "Unsupported image type. Allowed: " + ImageContentType.allowedLabels());
    }

    @Test
    void verifyImageContentType_acceptsJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        ImageContentType result = ImageFileValidator.verifyImageContentType(file);

        assertEquals(ImageContentType.JPEG, result);
        assertEquals("image/jpeg", result.mediaType());
        assertEquals(".jpg", result.extension());
    }

    @Test
    void verifyImageContentType_acceptsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        ImageContentType result = ImageFileValidator.verifyImageContentType(file);

        assertEquals(ImageContentType.PNG, result);
        assertEquals("image/png", result.mediaType());
        assertEquals(".png", result.extension());
    }

    @Test
    void verifyImageContentType_acceptsGif() {
        MockMultipartFile file =
                new MockMultipartFile("file", "animation.gif", "image/gif", GIF_BYTES);

        ImageContentType result = ImageFileValidator.verifyImageContentType(file);

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
                        () -> ImageFileValidator.verifyImageContentType(file));

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
                        () -> ImageFileValidator.verifyImageContentType(file));

        assertEquals("Wrong content Type", ex.getMessage());
    }

    private static MockMultipartFile validFile() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
    }

    private static Set<ConstraintViolation<ImageUploadRequest>> violations(ImageUploadRequest request) {
        return validator.validate(request);
    }

    private static void assertSingleViolation(ImageUploadRequest request, String message) {
        Set<ConstraintViolation<ImageUploadRequest>> violations = violations(request);
        assertEquals(1, violations.size());
        assertEquals(message, violations.iterator().next().getMessage());
    }
}
