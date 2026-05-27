package com.bowatt.instagramm.api.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bowatt.instagramm.api.config.AppConfig;
import com.bowatt.instagramm.api.config.WebMvcConfig;
import com.bowatt.instagramm.api.services.ImageService;
import com.bowatt.instagramm.api.web.ImageController;
import com.bowatt.instagramm.api.web.ImageExceptionHandler;
import com.bowatt.instagramm.api.web.ImageNotFoundException;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
                        "file", "photo.jpg", "image/jpeg", "image-bytes".getBytes());

        mockMvc.perform(
                        multipart("/api/images")
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
}
