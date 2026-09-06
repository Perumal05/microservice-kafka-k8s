package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.CartServiceClient;
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
public class RestCartServiceClient implements CartServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestCartServiceClient.class);
    private static final String SERVICE_NAME = "Cart Service";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final RestClient restClient;

    public RestCartServiceClient(@Qualifier("cartRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void clearCart(Long userId) {
        log.info("Clearing cart for userId={}", userId);
        try {
            restClient.delete()
                    .uri("/api/cart")
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Cart cleared for userId={}", userId);
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }
}
