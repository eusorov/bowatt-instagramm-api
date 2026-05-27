package com.bowatt.instagramm.api.web.dto;

public record ImageCreatedEvent(String type) {

    public static final String IMAGE_CREATED = "IMAGE_CREATED";

    public static ImageCreatedEvent imageCreated() {
        return new ImageCreatedEvent(IMAGE_CREATED);
    }
}
