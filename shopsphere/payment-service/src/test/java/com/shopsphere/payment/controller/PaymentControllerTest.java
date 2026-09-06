package com.shopsphere.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.SimulationMode;
import com.shopsphere.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_ShouldReturn201Created() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                100L, 50L, new BigDecimal("199.99"), "USD", PaymentMethod.CARD);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.paymentReference", startsWith("PAY-")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(199.99)))
                .andExpect(jsonPath("$.paymentMethod", is("CARD")));
    }

    @Test
    void getPaymentById_ShouldReturnPayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 50L, new BigDecimal("50.00"), "USD", PaymentMethod.UPI);
        MvcResult res = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long paymentId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(paymentId.intValue())))
                .andExpect(jsonPath("$.paymentMethod", is("UPI")));
    }

    @Test
    void processPayment_SimulateSuccess_ShouldReturnSuccess() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 50L, new BigDecimal("50.00"), "USD", PaymentMethod.UPI);
        MvcResult res = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long paymentId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        ProcessPaymentRequest processReq = new ProcessPaymentRequest(SimulationMode.SUCCESS, null);

        mockMvc.perform(post("/api/payments/{paymentId}/process", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void processPayment_SimulateFailure_ShouldReturnFailed() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 50L, new BigDecimal("50.00"), "USD", PaymentMethod.UPI);
        MvcResult res = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long paymentId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        ProcessPaymentRequest processReq = new ProcessPaymentRequest(SimulationMode.FAILURE, "Card declined");

        mockMvc.perform(post("/api/payments/{paymentId}/process", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.failureReason", is("Card declined")));
    }

    @Test
    void refundPayment_SuccessPayment_ShouldReturnRefunded() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 50L, new BigDecimal("50.00"), "USD", PaymentMethod.UPI);
        MvcResult res = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long paymentId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Process first to SUCCESS
        mockMvc.perform(post("/api/payments/{paymentId}/process", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProcessPaymentRequest(SimulationMode.SUCCESS, null))))
                .andExpect(status().isOk());

        // Refund
        mockMvc.perform(post("/api/payments/{paymentId}/refund", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")));
    }
}
