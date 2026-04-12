package com.assetdock.api.auth.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebSessionProperties.class)
public class WebSessionConfiguration {
}
