package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.dto.PaymentResult;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import com.shopsphere.order.model.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestPaymentServiceClientTest {

    private static final String SERVICE_NAME = "Payment Service";

    private MockRestServiceServer mockServer;
    private RestPaymentServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:4006")
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw RemoteExceptionTranslator.fromStatus(SERVICE_NAME, response.getStatusCode(),
                            new String(response.getBody().readAllBytes()));
                });
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestPaymentServiceClient(builder.build());
    }

    @Test
    void createPayment_Http201_ReturnsPendingPayment() {
        mockServer.expect(requestTo("http://localhost:4006/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":1,"paymentReference":"PAY-1","orderId":55,"userId":100,"amount":1200.00,"currency":"USD","status":"PENDING","paymentMethod":"CARD"}
                        """, MediaType.APPLICATION_JSON));

        PaymentResult result = client.createPayment(55L, 100L, new BigDecimal("1200.00"), "USD", PaymentMethod.CARD);

        assertEquals(1L, result.id());
        assertEquals("PENDING", result.status());
        assertFalse(result.isSuccessful());
        mockServer.verify();
    }

    @Test
    void processPayment_SimulatedSuccess_ReturnsHttp200WithSuccessStatus() {
        // Per stage-3 requirements: the Payment Service always returns HTTP 200 - the outcome
        // lives in the response body, never in the HTTP status.
        mockServer.expect(requestTo("http://localhost:4006/api/payments/1/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":1,"paymentReference":"PAY-1","orderId":55,"userId":100,"amount":1200.00,"currency":"USD","status":"SUCCESS"}
                        """, MediaType.APPLICATION_JSON));

        PaymentResult result = client.processPayment(1L);

        assertTrue(result.isSuccessful());
        mockServer.verify();
    }

    @Test
    void processPayment_SimulatedFailure_ReturnsHttp200WithFailedStatus() {
        mockServer.expect(requestTo("http://localhost:4006/api/payments/1/process"))
                .andRespond(withSuccess("""
                        {"id":1,"paymentReference":"PAY-1","orderId":55,"userId":100,"amount":1200.00,"currency":"USD","status":"FAILED","failureReason":"Simulated payment processing failure"}
                        """, MediaType.APPLICATION_JSON));

        PaymentResult result = client.processPayment(1L);

        assertFalse(result.isSuccessful());
        assertEquals("Simulated payment processing failure", result.failureReason());
        mockServer.verify();
    }
}
