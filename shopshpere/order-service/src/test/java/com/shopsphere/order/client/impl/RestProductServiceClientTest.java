package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.client.exception.RemoteResourceNotFoundException;
import com.shopsphere.order.client.exception.RemoteServiceBadRequestException;
import com.shopsphere.order.client.exception.RemoteServiceException;
import com.shopsphere.order.client.exception.RemoteServiceTimeoutException;
import com.shopsphere.order.client.exception.RemoteServiceUnavailableException;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestProductServiceClientTest {

    private static final String SERVICE_NAME = "Product Service";

    private MockRestServiceServer mockServer;
    private RestProductServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:4002")
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw RemoteExceptionTranslator.fromStatus(SERVICE_NAME, response.getStatusCode(),
                            new String(response.getBody().readAllBytes()));
                });
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestProductServiceClient(builder.build());
    }

    @Test
    void getProduct_Http200_ReturnsProduct() {
        mockServer.expect(requestTo("http://localhost:4002/api/products/10"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":10,"sku":"SKU-100","name":"Laptop","price":750.00,"currency":"USD","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));

        ProductClientResponse response = client.getProduct(10L);

        assertEquals(10L, response.id());
        assertEquals("SKU-100", response.sku());
        assertTrue(response.isActive());
        mockServer.verify();
    }

    @Test
    void getProduct_Http404_ThrowsRemoteResourceNotFoundException() {
        mockServer.expect(requestTo("http://localhost:4002/api/products/999"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Product not found\"}"));

        assertThrows(RemoteResourceNotFoundException.class, () -> client.getProduct(999L));
        mockServer.verify();
    }

    @Test
    void getProduct_Http400_ThrowsRemoteServiceBadRequestException() {
        mockServer.expect(requestTo("http://localhost:4002/api/products/10"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"bad request\"}"));

        assertThrows(RemoteServiceBadRequestException.class, () -> client.getProduct(10L));
        mockServer.verify();
    }

    @Test
    void getProduct_Http500_ThrowsRemoteServiceException() {
        mockServer.expect(requestTo("http://localhost:4002/api/products/10"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"boom\"}"));

        RemoteServiceException ex = assertThrows(RemoteServiceException.class, () -> client.getProduct(10L));
        assertFalse(ex instanceof RemoteResourceNotFoundException);
        assertFalse(ex instanceof RemoteServiceBadRequestException);
        mockServer.verify();
    }

    @Test
    void getProduct_MalformedResponse_ThrowsRemoteServiceException() {
        mockServer.expect(requestTo("http://localhost:4002/api/products/10"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThrows(RemoteServiceException.class, () -> client.getProduct(10L));
        mockServer.verify();
    }

    @Test
    void getProduct_ConnectionFailure_ThrowsRemoteServiceUnavailableException() {
        // Point at a client whose backing RestClient targets an unroutable address to simulate
        // a connection failure rather than going through MockRestServiceServer.
        RestClient unreachableClient = RestClient.builder()
                .baseUrl("http://localhost:1")
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        RestProductServiceClient unreachable = new RestProductServiceClient(unreachableClient);

        assertThrows(RemoteServiceUnavailableException.class, () -> unreachable.getProduct(10L));
    }

    @Test
    void translator_TimeoutCause_ProducesTimeoutException() {
        RemoteServiceException ex = RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, new SocketTimeoutException("read timed out"));
        assertInstanceOf(RemoteServiceTimeoutException.class, ex);
    }
}
