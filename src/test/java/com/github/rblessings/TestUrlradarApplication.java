package com.github.rblessings;

import org.springframework.boot.SpringApplication;

public class TestUrlradarApplication {

    public static void main(String[] args) {
        SpringApplication.from(UrlradarApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
