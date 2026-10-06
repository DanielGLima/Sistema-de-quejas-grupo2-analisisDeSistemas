package com.sistemadequejas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaDeQuejasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaDeQuejasApplication.class, args);
    }

}
