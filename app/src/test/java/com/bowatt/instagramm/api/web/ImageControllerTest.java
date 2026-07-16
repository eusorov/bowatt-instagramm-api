package com.bowatt.instagramm.api.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bowatt.instagramm.api.config.AppConfig;
import com.bowatt.instagramm.api.config.WebMvcConfig;
import com.bowatt.instagramm.api.services.ImageService;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
@Import({AppConfig.class, WebMvcConfig.class, ImageExceptionHandler.class})
@TestPropertySource(properties = "app.storage.upload-dir=/tmp/uploads")
class ImageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ImageService imageService;

    @Test
    void uploadImageReturnsCreatedResponse() throws Exception {
        Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
        Long id = 1L;
        ImageResponse response =
                new ImageResponse(
                        id,
                        "photo.jpg",
                        "image/jpeg",
                        128L,
                        "sunset",
                        Set.of("beach", "summer"),
                        "/images/photo.jpg",
                        createdAt);

        when(imageService.upload(any(), eq("sunset"), eq(Set.of("beach", "summer"))))
                .thenReturn(response);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "photo.jpg", 
                        "image/jpeg", 
                        "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .param("title", "sunset")
                                .param("tags", "beach", "summer")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.originalFilename").value("photo.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.sizeBytes").value(128))
                .andExpect(jsonPath("$.title").value("sunset"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.url").value("/images/photo.jpg"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-27T10:00:00Z"));

        verify(imageService).upload(any(), eq("sunset"), eq(Set.of("beach", "summer")));
    }

    @Test
    void uploadImageRejectsEmptyFile() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .param("title", "title")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image file is required"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadImageRejectsMissingContentType() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        null,
                        "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image content type is required"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadImageRejectsMissingOriginalFilename() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        null,
                        "image/jpeg",
                        "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image original filename is required"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadImageRejectsOriginalFilenameLongerThan255Characters() throws Exception {
        String longName = "a".repeat(256);
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        longName,
                        "image/jpeg",
                        "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .param("title", "title")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Original filename must be at most 255 characters"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadImageRejectsTitleLongerThan255Characters() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .param("title", "a".repeat(256))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must be at most 255 characters"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadImageRejectsMoreThanTenTags() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        "image-bytes".getBytes(StandardCharsets.UTF_8));
        String[] elevenTags = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toArray(String[]::new);

        mockMvc.perform(
                        multipart("/api/uploads")
                                .file(file)
                                .param("tags", elevenTags)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tags must be at most 10"));

        verify(imageService, never()).upload(any(), any(), any());
    }

    @Test
    void listImagesReturnsPagedResponseWithTags() throws Exception {
        Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
        Page page =
                new Page(
                        List.of(
                                new ImageResponse(
                                        1L,
                                        "photo.jpg",
                                        "image/jpeg",
                                        128L,
                                        "sunset",
                                        Set.of("beach", "summer"),
                                        "/images/photo.jpg.jpg",
                                        createdAt)),
                        0,
                        20,
                        1,
                        1);

        when(imageService.list(any(Pageable.class), eq(null))).thenReturn(page);

        mockMvc.perform(get("/api/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("sunset"))
                .andExpect(jsonPath("$.content[0].tags.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(imageService).list(any(Pageable.class), eq(null));
    }

    @Test
    void getImageByIdReturnsNotFoundWhenMissing() throws Exception {
        when(imageService.getImageById(99L))
                .thenThrow(new ImageNotFoundException("Image not found with id: 99"));

        mockMvc.perform(get("/api/images/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Image not found with id: 99"));
    }

    @Test
    void unknownRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found: api/unknown"));
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/images"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("Method not allowed: DELETE")))
                .andExpect(jsonPath("$.message", containsString("GET")));
    }
}
