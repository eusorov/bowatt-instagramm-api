package com.bowatt.instagramm.api.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bowatt.instagramm.api.config.AppConfig;
import com.bowatt.instagramm.api.config.WebMvcConfig;
import com.bowatt.instagramm.api.services.TagService;
import com.bowatt.instagramm.api.web.dto.TagResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
@Import({AppConfig.class, WebMvcConfig.class, ImageExceptionHandler.class})
@TestPropertySource(properties = "app.storage.upload-dir=/tmp/uploads")
class ApiVersionTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TagService tagService;

    @Test
    void usesDefaultVersionWhenHeaderIsMissing() throws Exception {
        when(tagService.listAll()).thenReturn(List.of(new TagResponse(1L, "beach")));

        mockMvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void acceptsExplicitVersionHeader() throws Exception {
        when(tagService.listAll()).thenReturn(List.of(new TagResponse(1L, "beach")));

        mockMvc.perform(get("/api/tags").header("API-Version", "1.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsUnsupportedVersionHeader() throws Exception {
        mockMvc.perform(get("/api/tags").header("API-Version", "2.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
