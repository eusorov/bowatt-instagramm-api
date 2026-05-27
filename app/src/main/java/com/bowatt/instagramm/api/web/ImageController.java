package com.bowatt.instagramm.api.web;

import com.bowatt.instagramm.api.services.ImageService;
import com.bowatt.instagramm.api.web.dto.ImageResponse;
import com.bowatt.instagramm.api.web.dto.ImageResponse.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Instagram image upload API")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an image")
    public ImageResponse uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("tags") Set<String> tags) {
        return imageService.upload(file, title, tags);
    }

    @GetMapping
    @Operation(summary = "List images")
    public Page listImages(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return imageService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an image by ID")
    public ImageResponse getImageById(@PathVariable Long id) {
        return imageService.getImageById(id);
    }   
}
