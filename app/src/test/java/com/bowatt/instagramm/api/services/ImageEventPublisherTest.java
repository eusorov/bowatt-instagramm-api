package com.bowatt.instagramm.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.bowatt.instagramm.api.web.dto.ImageCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ImageEventPublisherTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private ImageEventPublisher imageEventPublisher;

    @BeforeEach
    void setUp() {
        imageEventPublisher = new ImageEventPublisher(messagingTemplate);
    }

    @Test
    void publishImageCreated_sendsImageCreatedEventToImagesTopic() {
        imageEventPublisher.publishImageCreated();

        ArgumentCaptor<ImageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ImageCreatedEvent.class);
        verify(messagingTemplate)
                .convertAndSend(eq(ImageEventPublisher.IMAGES_TOPIC), eventCaptor.capture());

        assertEquals(ImageCreatedEvent.IMAGE_CREATED, eventCaptor.getValue().type());
    }
}
