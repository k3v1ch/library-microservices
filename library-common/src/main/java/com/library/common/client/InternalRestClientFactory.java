package com.library.common.client;

import com.library.common.api.CorrelationIdFilter;
import com.library.common.config.LibraryProperties;
import org.slf4j.MDC;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/** Фабрика HTTP-клиентов для внутренних вызовов между сервисами. */
public class InternalRestClientFactory {

    private final RestClient.Builder builder;
    private final LibraryProperties properties;
    private final ServiceTokenProvider tokenProvider;

    public InternalRestClientFactory(RestClient.Builder builder,
                                     LibraryProperties properties,
                                     ServiceTokenProvider tokenProvider) {
        this.builder = builder;
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    public RestClient forBaseUrl(String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .requestInterceptor((request, body, execution) -> {
                    if (tokenProvider != null) {
                        request.getHeaders().setBearerAuth(tokenProvider.token());
                    }
                    String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().add(CorrelationIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getHttp().getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getHttp().getReadTimeout());
        return factory;
    }
}
