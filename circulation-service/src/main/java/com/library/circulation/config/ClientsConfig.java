package com.library.circulation.config;

import com.library.common.client.InternalRestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** HTTP-клиенты к соседним сервисам. */
@Configuration
public class ClientsConfig {

    @Bean
    public RestClient catalogRestClient(InternalRestClientFactory factory, CirculationProperties properties) {
        return factory.forBaseUrl(properties.getServices().getCatalogUrl());
    }

    @Bean
    public RestClient readersRestClient(InternalRestClientFactory factory, CirculationProperties properties) {
        return factory.forBaseUrl(properties.getServices().getReadersUrl());
    }

    @Bean
    public RestClient notificationsRestClient(InternalRestClientFactory factory, CirculationProperties properties) {
        return factory.forBaseUrl(properties.getServices().getNotificationsUrl());
    }
}
