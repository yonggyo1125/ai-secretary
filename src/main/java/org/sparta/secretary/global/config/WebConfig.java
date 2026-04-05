package org.sparta.secretary.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FileProperties.class)
public class WebConfig implements WebFluxConfigurer {

    private final FileProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(properties.path() + "/**")
                .addResourceLocations("file:///" + properties.path() + "/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
}
