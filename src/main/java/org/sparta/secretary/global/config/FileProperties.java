package org.sparta.secretary.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("file.upload")
public record FileProperties(
        String url,
        String path
) {}
