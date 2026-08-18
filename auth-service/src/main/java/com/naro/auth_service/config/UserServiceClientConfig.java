package com.naro.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceClientConfig {

    @Bean
    public RestClient userServiceRestClient(
        RestClient.Builder builder,
        @Value("${services.user-service.url}") String userServiceUrl
    ) {
        return builder.baseUrl(userServiceUrl).build();
    }

}
