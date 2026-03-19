package com.ramani.banking.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${services.account-service.url:http://localhost:8083}")
    private String accountServiceUrl;

    @Bean
    public RestClient accountServiceClient() {
        return RestClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }
}
