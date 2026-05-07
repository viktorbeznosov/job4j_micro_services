package com.example.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }
}
