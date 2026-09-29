package com.library.borrow.config;

import com.library.common.client.InternalRestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** HTTP-клиенты к соседним сервисам. */
@Configuration
public class ClientsConfig {

    @Bean
    public RestClient bookRestClient(InternalRestClientFactory factory, BorrowProperties properties) {
        return factory.forBaseUrl(properties.getServices().getBookUrl());
    }

    @Bean
    public RestClient userRestClient(InternalRestClientFactory factory, BorrowProperties properties) {
        return factory.forBaseUrl(properties.getServices().getUserUrl());
    }

    @Bean
    public RestClient notificationsRestClient(InternalRestClientFactory factory, BorrowProperties properties) {
        return factory.forBaseUrl(properties.getServices().getNotificationsUrl());
    }
}
