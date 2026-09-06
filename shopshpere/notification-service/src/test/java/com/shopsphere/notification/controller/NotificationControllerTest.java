package com.shopsphere.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    private CreateNotificationRequest emailRequest() {
        return new CreateNotificationRequest(
                10L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL,
                "user@example.com", "Order Confirmation", "Your order #123 has been placed.", null);
    }

    private Long createNotification() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createNotification_ShouldReturn201WithPendingStatus() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.channel", is("EMAIL")))
                .andExpect(jsonPath("$.recipient", is("user@example.com")))
                .andExpect(jsonPath("$.type", is("ORDER_CREATED")));
    }

    @Test
    void getNotificationById_ShouldReturnNotification() throws Exception {
        Long id = createNotification();

        mockMvc.perform(get("/api/notifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void getNotificationById_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/notifications/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void getNotificationsByUserId_ShouldReturnPagedHistory() throws Exception {
        createNotification();

        mockMvc.perform(get("/api/notifications/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(10)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void sendNotification_Success_ShouldReturnSentStatus() throws Exception {
        Long id = createNotification();

        mockMvc.perform(post("/api/notifications/{id}/send", id)
                        .param("simulateFailure", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SENT")))
                .andExpect(jsonPath("$.sentAt", notNullValue()));
    }

    @Test
    void sendNotification_SimulatedFailure_ShouldReturnFailedStatus() throws Exception {
        Long id = createNotification();

        mockMvc.perform(post("/api/notifications/{id}/send", id)
                        .param("simulateFailure", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.failureReason", containsString("Simulated EMAIL delivery failure")));
    }

    @Test
    void sendNotification_AlreadySent_ShouldReturn400() throws Exception {
        Long id = createNotification();

        // Send first time
        mockMvc.perform(post("/api/notifications/{id}/send", id)
                        .param("simulateFailure", "false"))
                .andExpect(status().isOk());

        // Attempt to send again
        mockMvc.perform(post("/api/notifications/{id}/send", id)
                        .param("simulateFailure", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_NOTIFICATION_STATE")));
    }

    @Test
    void cancelNotification_Pending_ShouldReturnCancelledStatus() throws Exception {
        Long id = createNotification();

        mockMvc.perform(post("/api/notifications/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void cancelNotification_AlreadySent_ShouldReturn400() throws Exception {
        Long id = createNotification();

        // Send first
        mockMvc.perform(post("/api/notifications/{id}/send", id)
                        .param("simulateFailure", "false"))
                .andExpect(status().isOk());

        // Try to cancel SENT notification
        mockMvc.perform(post("/api/notifications/{id}/cancel", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_NOTIFICATION_STATE")));
    }

    @Test
    void createNotification_ValidationError_MissingFields_ShouldReturn400() throws Exception {
        String invalidJson = """
                {
                  "userId": null,
                  "type": "ORDER_CREATED",
                  "channel": "EMAIL",
                  "recipient": "",
                  "message": ""
                }
                """;

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }
}
