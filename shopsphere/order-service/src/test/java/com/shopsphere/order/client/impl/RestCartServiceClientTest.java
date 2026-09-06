package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.exception.RemoteServiceException;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestCartServiceClientTest {

    private static final String SERVICE_NAME = "Cart Service";

    private MockRestServiceServer mockServer;
    private RestCartServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:4003")
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw RemoteExceptionTranslator.fromStatus(SERVICE_NAME, response.getStatusCode(),
                            new String(response.getBody().readAllBytes()));
                });
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestCartServiceClient(builder.build());
    }

    @Test
    void clearCart_Http204_CompletesSuccessfully() {
        mockServer.expect(requestTo("http://localhost:4003/api/cart"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-User-Id", "100"))
                .andRespond(withNoContent());

        client.clearCart(100L);

        mockServer.verify();
    }

    @Test
    void clearCart_Http500_ThrowsRemoteServiceException() {
        mockServer.expect(requestTo("http://localhost:4003/api/cart"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"boom\"}"));

        assertThrows(RemoteServiceException.class, () -> client.clearCart(100L));
        mockServer.verify();
    }
}
