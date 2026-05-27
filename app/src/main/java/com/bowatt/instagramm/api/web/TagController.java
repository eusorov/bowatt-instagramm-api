package com.bowatt.instagramm.api.web;

import com.bowatt.instagramm.api.services.TagService;
import com.bowatt.instagramm.api.web.dto.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Image tag API")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "List all tags")
    public List<TagResponse> listTags() {
        return tagService.listAll();
    }
}
