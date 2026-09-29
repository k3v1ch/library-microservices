package com.library.common.config;

import com.library.common.api.ApiExceptionHandler;
import com.library.common.api.CorrelationIdFilter;
import com.library.common.client.InternalRestClientFactory;
import com.library.common.client.ServiceTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;

/** Автоконфигурация общей инфраструктуры для всех сервисов. */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(LibraryProperties.class)
public class LibraryCommonAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Bean
    @ConditionalOnProperty(prefix = "library.auth", name = "base-url")
    public ServiceTokenProvider serviceTokenProvider(ObjectProvider<RestClient.Builder> builders,
                                                     LibraryProperties properties) {
        // Тот же билдер, что и у остальных клиентов: с Eureka адрес auth-service ищется по имени.
        RestClient authClient = builders.getIfAvailable(RestClient::builder)
                .clone()
                .baseUrl(properties.getAuth().getBaseUrl())
                .build();
        return new ServiceTokenProvider(authClient, properties.getAuth());
    }

    @Bean
    @ConditionalOnMissingBean
    public InternalRestClientFactory internalRestClientFactory(
            ObjectProvider<RestClient.Builder> builders,
            LibraryProperties properties,
            ObjectProvider<ServiceTokenProvider> tokenProvider) {
        RestClient.Builder builder = builders.getIfAvailable(RestClient::builder);
        return new InternalRestClientFactory(builder, properties, tokenProvider.getIfAvailable());
    }
}
