package com.library.readers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReadersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadersServiceApplication.class, args);
    }
}
