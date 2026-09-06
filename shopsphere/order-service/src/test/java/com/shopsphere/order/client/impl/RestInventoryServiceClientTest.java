package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.dto.InventoryReservationResult;
import com.shopsphere.order.client.exception.RemoteServiceBadRequestException;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestInventoryServiceClientTest {

    private static final String SERVICE_NAME = "Inventory Service";

    private MockRestServiceServer mockServer;
    private RestInventoryServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:4005")
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw RemoteExceptionTranslator.fromStatus(SERVICE_NAME, response.getStatusCode(),
                            new String(response.getBody().readAllBytes()));
                });
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestInventoryServiceClient(builder.build());
    }

    @Test
    void reserve_Http200_ReturnsReservation() {
        mockServer.expect(requestTo("http://localhost:4005/api/inventory/10/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":101,"inventoryId":1,"orderId":55,"quantity":2,"status":"RESERVED"}
                        """, MediaType.APPLICATION_JSON));

        InventoryReservationResult result = client.reserve(10L, 55L, 2);

        assertEquals(101L, result.reservationId());
        assertEquals("RESERVED", result.status());
        mockServer.verify();
    }

    @Test
    void reserve_InsufficientStock_ThrowsRemoteServiceBadRequestException() {
        mockServer.expect(requestTo("http://localhost:4005/api/inventory/10/reserve"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"INSUFFICIENT_INVENTORY\"}"));

        assertThrows(RemoteServiceBadRequestException.class, () -> client.reserve(10L, 55L, 2));
        mockServer.verify();
    }

    @Test
    void release_Http200_CompletesSuccessfully() {
        mockServer.expect(requestTo("http://localhost:4005/api/inventory/reservations/101/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":101,"inventoryId":1,"orderId":55,"quantity":2,"status":"RELEASED"}
                        """, MediaType.APPLICATION_JSON));

        client.release(101L, "Checkout compensation");

        mockServer.verify();
    }
}
