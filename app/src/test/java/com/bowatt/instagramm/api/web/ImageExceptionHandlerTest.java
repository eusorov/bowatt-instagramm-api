package com.bowatt.instagramm.api.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ImageExceptionHandlerTest {

    private final ImageExceptionHandler handler = new ImageExceptionHandler();

    @Test
    void handleMaxUploadSizeExceededException_returnsPayloadTooLargeWithMessage() {
        var response =
                handler.handleMaxUploadSizeExceededException(
                        new MaxUploadSizeExceededException(10 * 1024 * 1024));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Maximum upload size 10MB exceeded", response.getBody().get("message"));
    }
}
