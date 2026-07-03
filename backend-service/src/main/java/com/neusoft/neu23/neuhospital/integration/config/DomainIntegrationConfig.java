package com.neusoft.neu23.neuhospital.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DomainIntegrationProperties.class)
public class DomainIntegrationConfig {
}
