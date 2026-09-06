package com.shopsphere.order.client.impl;

import com.shopsphere.order.client.InventoryServiceClient;
import com.shopsphere.order.client.dto.InventoryReservationResult;
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
public class RestInventoryServiceClient implements InventoryServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestInventoryServiceClient.class);
    private static final String SERVICE_NAME = "Inventory Service";

    private final RestClient restClient;

    public RestInventoryServiceClient(@Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public InventoryReservationResult reserve(Long productId, Long orderId, Integer quantity) {
        log.info("Reserving inventory productId={} quantity={} orderId={}", productId, quantity, orderId);
        try {
            ReservationApiResponse response = restClient.post()
                    .uri("/api/inventory/{productId}/reserve", productId)
                    .body(new ReserveApiRequest(orderId, quantity))
                    .retrieve()
                    .body(ReservationApiResponse.class);

            InventoryReservationResult result = toResult(response);
            log.info("Inventory reservation successful reservationId={}", result.reservationId());
            return result;
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }

    @Override
    public void release(Long reservationId, String reason) {
        log.info("Releasing inventory reservationId={}", reservationId);
        try {
            restClient.post()
                    .uri("/api/inventory/reservations/{reservationId}/release", reservationId)
                    .body(new ReleaseApiRequest(reason))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Inventory reservation {} released", reservationId);
        } catch (ResourceAccessException ex) {
            throw RemoteExceptionTranslator.fromIoFailure(SERVICE_NAME, ex);
        } catch (RemoteServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RemoteServiceException(SERVICE_NAME, "Unexpected error calling " + SERVICE_NAME, ex);
        }
    }

    private InventoryReservationResult toResult(ReservationApiResponse response) {
        if (response == null) {
            return null;
        }
        return new InventoryReservationResult(
                response.id(), response.inventoryId(), response.orderId(), response.quantity(), response.status());
    }

    /** Mirrors the Inventory Service's reservation request shape - kept private to this client. */
    private record ReserveApiRequest(Long orderId, Integer quantity) {}

    /** Mirrors the Inventory Service's release request shape - kept private to this client. */
    private record ReleaseApiRequest(String reason) {}

    /** Mirrors the Inventory Service's reservation response shape - kept private to this client. */
    private record ReservationApiResponse(Long id, Long inventoryId, Long orderId, Integer quantity, String status) {}
}
