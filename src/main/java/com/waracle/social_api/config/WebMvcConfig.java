package com.waracle.social_api.config;

import com.waracle.social_api.service.user.ProfilePictureStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ProfilePictureStorageService profilePictureStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPrefix = profilePictureStorageService.getUrlPrefix();
        String location = profilePictureStorageService.getUploadPath().toUri().toString();

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
