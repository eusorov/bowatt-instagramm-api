package com.bowatt.instagramm.api.web;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ImageContentType {
    JPEG("image/jpeg", ".jpg", "JPEG"),
    PNG("image/png", ".png", "PNG"),
    GIF("image/gif", ".gif", "GIF");

    private final String mediaType;
    private final String extension;
    private final String label;

    ImageContentType(String mediaType, String extension, String label) {
        this.mediaType = mediaType;
        this.extension = extension;
        this.label = label;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }

    public String label() {
        return label;
    }

    public static Optional<ImageContentType> fromMediaType(String contentType) {
        return fromMediaType(contentType, false);
    }

    public static Optional<ImageContentType> fromMediaType(String contentType, boolean throwException) {
        if (contentType == null) {
            return Optional.empty();
        }

        for (ImageContentType type : values()) {
            if (type.mediaType.equals(contentType)) {
                return Optional.of(type);
            }
        }

        if (throwException) {
            throw new ImageUploadException(
                    "Unsupported image type. Allowed: "
                            + ImageContentType.allowedLabels());
        }
        return Optional.empty();
    }

    public static String allowedLabels() {
        return Arrays.stream(values()).map(ImageContentType::label).collect(Collectors.joining(", "));
    }
}
