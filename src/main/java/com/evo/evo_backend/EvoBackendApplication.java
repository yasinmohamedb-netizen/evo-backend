package com.evo.evo_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // Don't forget this import!

@SpringBootApplication
@EnableCaching // This turns on the Redis caching logic
public class EvoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvoBackendApplication.class, args);
    }

}