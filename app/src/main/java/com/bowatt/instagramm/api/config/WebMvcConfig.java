package com.bowatt.instagramm.api.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public WebMvcConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation =
                Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize().toUri().toString();
        if (!uploadLocation.endsWith("/")) {
            uploadLocation = uploadLocation + "/";
        }

        registry.addResourceHandler("/images/**").addResourceLocations(uploadLocation);
    }
}
