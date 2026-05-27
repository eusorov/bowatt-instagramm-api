package com.bowatt.instagramm.api.services;

import com.bowatt.instagramm.api.web.dto.ImageCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ImageEventPublisher {

    public static final String IMAGES_TOPIC = "/topic/images";

    private final SimpMessagingTemplate messagingTemplate;

    public ImageEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishImageCreated() {
        messagingTemplate.convertAndSend(IMAGES_TOPIC, ImageCreatedEvent.imageCreated());
    }
}
