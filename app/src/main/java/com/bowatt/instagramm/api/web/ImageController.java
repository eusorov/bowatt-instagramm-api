package com.bowatt.instagramm.api.web;

import com.bowatt.instagramm.api.services.ImageService;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;
import com.bowatt.instagramm.api.web.dto.ImageUploadRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(version = "1.00")
@Tag(name = "Images", description = "Instagram image upload API")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(path = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an image")
    public ImageResponse uploadImage(@Valid ImageUploadRequest request) {
        return imageService.upload(
                Objects.requireNonNull(request.getFile()),
                request.getTitle(),
                request.getTags());
    }

    @GetMapping("/api/images")
    @Operation(summary = "List images")
    public Page listImages(
            @RequestParam(value = "tags", required = false) Set<String> tags,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC, page = 0)
                    Pageable pageable) {
        return imageService.list(pageable, tags);
    }

    @GetMapping("/api/images/{id}")
    @Operation(summary = "Get an image by ID")
    public ImageResponse getImageById(@PathVariable Long id) {
        return imageService.getImageById(id);
    }   
}
