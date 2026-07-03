package com.neusoft.neu23.neuhospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.neusoft.neu23.neuhospital.ct.config.CtAnalysisProperties;
import com.neusoft.neu23.neuhospital.ct.config.MinioProperties;
import com.neusoft.neu23.neuhospital.integration.config.DomainIntegrationProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({CtAnalysisProperties.class, MinioProperties.class, DomainIntegrationProperties.class})
public class BackendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendServiceApplication.class, args);
    }

}
