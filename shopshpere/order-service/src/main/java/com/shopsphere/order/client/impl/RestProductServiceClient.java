package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.ProductServiceClient;
import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.client.exception.RemoteServiceException;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestProductServiceClient implements ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestProductServiceClient.class);
    private static final String SERVICE_NAME = "Product Service";

    private final RestClient restClient;

    public RestProductServiceClient(@Qualifier("productRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProductClientResponse getProduct(Long productId) {
        log.info("Calling Product Service for productId={}", productId);
        try {
            ProductClientResponse response = restClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(ProductClientResponse.class);
            log.info("Product Service returned productId={}", productId);
            return response;
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }
}
