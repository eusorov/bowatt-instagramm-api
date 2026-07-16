package com.bowatt.instagramm.api.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadRequestValidationTest {

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
                        new MockMultipartFile(
                                "file",
                                "photo.jpg",
                                null,
                                "image-bytes".getBytes(StandardCharsets.UTF_8)),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image content type is required");
    }

    @Test
    void rejectsNullOriginalFilename() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile(
                                "file",
                                null,
                                "image/jpeg",
                                "image-bytes".getBytes(StandardCharsets.UTF_8)),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image original filename is required");
    }

    @Test
    void rejectsEmptyOriginalFilename() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile(
                                "file",
                                "",
                                "image/jpeg",
                                "image-bytes".getBytes(StandardCharsets.UTF_8)),
                        "title",
                        Set.of());

        assertSingleViolation(request, "Image original filename is required");
    }

    @Test
    void rejectsOriginalFilenameLongerThan255Characters() {
        var request =
                new ImageUploadRequest(
                        new MockMultipartFile(
                                "file",
                                "a".repeat(256),
                                "image/jpeg",
                                "image-bytes".getBytes(StandardCharsets.UTF_8)),
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

    private static MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "image-bytes".getBytes(StandardCharsets.UTF_8));
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
