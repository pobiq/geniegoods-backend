package com.example.geniegoods.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스 핸들러 설정
 * 로컬에 저장된 이미지를 URL로 접근할 수 있게 설정합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.path}")
    private String storagePath;

    @Value("${app.storage.resource-handler}")
    private String resourceHandler;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file:/// 접두사가 필요합니다.
        String location = "file:///" + storagePath + "/";
        
        registry.addResourceHandler(resourceHandler)
                .addResourceLocations(location);
    }
}
