package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.PaymentServiceClient;
import com.shopsphere.order.client.dto.PaymentResult;
import com.shopsphere.order.client.exception.RemoteServiceException;
import com.shopsphere.order.client.support.RemoteExceptionTranslator;
import com.shopsphere.order.model.entity.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * The Payment Service always responds with HTTP 200 for both create and
 * process calls - the outcome (SUCCESS/FAILED) lives in the response body.
 * This client therefore never throws for a business-level payment failure;
 * callers must inspect {@link PaymentResult#isSuccessful()}.
 */
@Component
public class RestPaymentServiceClient implements PaymentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestPaymentServiceClient.class);
    private static final String SERVICE_NAME = "Payment Service";

    private final RestClient restClient;

    public RestPaymentServiceClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PaymentResult createPayment(Long orderId, Long userId, BigDecimal amount, String currency, PaymentMethod paymentMethod) {
        log.info("Creating payment intent for orderId={} amount={} {}", orderId, amount, currency);
        try {
            PaymentApiResponse response = restClient.post()
                    .uri("/api/payments")
                    .body(new CreatePaymentApiRequest(orderId, userId, amount, currency, paymentMethod.name()))
                    .retrieve()
                    .body(PaymentApiResponse.class);
            return toResult(response);
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }

    @Override
    public PaymentResult processPayment(Long paymentId) {
        log.info("Processing payment paymentId={}", paymentId);
        try {
            // No request body -> Payment Service defaults simulationMode to SUCCESS.
            PaymentApiResponse response = restClient.post()
                    .uri("/api/payments/{paymentId}/process", paymentId)
                    .retrieve()
                    .body(PaymentApiResponse.class);
            PaymentResult result = toResult(response);
            log.info("Payment {} for paymentId={}", result.isSuccessful() ? "successful" : "failed", paymentId);
            return result;
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }

    private PaymentResult toResult(PaymentApiResponse response) {
        if (response == null) {
            return null;
        }
        return new PaymentResult(
                response.id(), response.paymentReference(), response.orderId(), response.userId(),
                response.amount(), response.currency(), response.status(), response.failureReason());
    }

    /** Mirrors the Payment Service's create-payment request shape - kept private to this client. */
    private record CreatePaymentApiRequest(Long orderId, Long userId, BigDecimal amount, String currency, String paymentMethod) {}

    /** Mirrors the fields of the Payment Service's response we actually need - kept private to this client. */
    private record PaymentApiResponse(
            Long id, String paymentReference, Long orderId, Long userId,
            BigDecimal amount, String currency, String status, String failureReason) {}
}
