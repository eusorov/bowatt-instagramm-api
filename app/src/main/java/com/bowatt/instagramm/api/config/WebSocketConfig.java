package com.bowatt.instagramm.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;

    public WebSocketConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws");
        if (corsProperties.allowedOriginPatterns() != null
                && !corsProperties.allowedOriginPatterns().isEmpty()) {
            endpoint.setAllowedOriginPatterns(
                    corsProperties.allowedOriginPatterns().toArray(String[]::new));
        } else if (corsProperties.allowedOrigins() != null
                && !corsProperties.allowedOrigins().isEmpty()) {
            endpoint.setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
        }
    }
}
