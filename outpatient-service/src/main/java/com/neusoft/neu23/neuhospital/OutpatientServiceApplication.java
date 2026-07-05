package com.neusoft.neu23.neuhospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OutpatientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutpatientServiceApplication.class, args);
    }
}
