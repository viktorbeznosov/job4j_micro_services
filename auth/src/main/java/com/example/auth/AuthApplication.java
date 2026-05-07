package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.example.auth.service.DataSeeder;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class AuthApplication {

    private final DataSeeder dataSeeder;

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        dataSeeder.seed();
    }
}
