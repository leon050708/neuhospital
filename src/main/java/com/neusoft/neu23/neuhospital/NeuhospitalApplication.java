package com.neusoft.neu23.neuhospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.neusoft.neu23.neuhospital.ct.config.CtAnalysisProperties;
import com.neusoft.neu23.neuhospital.ct.config.MinioProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({CtAnalysisProperties.class, MinioProperties.class})
public class NeuhospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeuhospitalApplication.class, args);
    }

}
