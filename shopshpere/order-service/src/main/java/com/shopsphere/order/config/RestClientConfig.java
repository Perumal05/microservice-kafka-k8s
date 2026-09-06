package com.shopsphere.order.config;

import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Builds one {@link RestClient} bean per downstream service, each configured
 * with the service's base URL, connect/read timeouts, correlation-ID
 * propagation, and a default error handler that translates non-2xx
 * responses into the {@code Remote*Exception} hierarchy.
 * <p>
 * Deliberately NOT using OpenFeign - Spring's RestClient is used directly so
 * that connection handling, timeouts, and error translation are all
 * explicit and easy to reason about at this stage of the project.
 */
@Configuration
@EnableConfigurationProperties({ServiceUrlProperties.class, HttpClientProperties.class})
public class RestClientConfig {

    private final HttpClientProperties httpClientProperties;

    public RestClientConfig(HttpClientProperties httpClientProperties) {
        this.httpClientProperties = httpClientProperties;
    }

    @Bean
    public RestClient productRestClient(ServiceUrlProperties serviceUrlProperties) {
        return buildClient(serviceUrlProperties.getProduct().getUrl(), "Product Service");
    }

    @Bean
    public RestClient inventoryRestClient(ServiceUrlProperties serviceUrlProperties) {
        return buildClient(serviceUrlProperties.getInventory().getUrl(), "Inventory Service");
    }

    @Bean
    public RestClient paymentRestClient(ServiceUrlProperties serviceUrlProperties) {
        return buildClient(serviceUrlProperties.getPayment().getUrl(), "Payment Service");
    }

    @Bean
    public RestClient cartRestClient(ServiceUrlProperties serviceUrlProperties) {
        return buildClient(serviceUrlProperties.getCart().getUrl(), "Cart Service");
    }

    private RestClient buildClient(String baseUrl, String serviceName) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .requestInterceptor(new CorrelationIdPropagationInterceptor())
                .defaultStatusHandler(
                        status -> status.isError(),
                        (request, response) -> {
                            String body = readBodySafely(response);
                            throw RemoteExceptionTranslator.fromStatus(serviceName, response.getStatusCode(), body);
                        })
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(httpClientProperties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofMillis(httpClientProperties.getReadTimeoutMs()));
        return factory;
    }

    private String readBodySafely(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(unable to read response body)";
        }
    }
}
