package com.microservice.paymentservice.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.microservice.paymentservice.JwtUtils;
import com.microservice.paymentservice.model.TransactionDetails;
import com.microservice.paymentservice.payload.PaymentRequest;
import com.microservice.paymentservice.payload.PaymentResponse;
import com.microservice.paymentservice.repository.TransactionDetailsRepository;
import com.microservice.paymentservice.service.PaymentService;
import com.microservice.paymentservice.utils.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest({"server.port=0"})
@AutoConfigureMockMvc
@EnableConfigurationProperties
@ActiveProfiles("test")
public class PaymentControllerTest {

    @RegisterExtension
    static WireMockExtension wireMockServer
            = WireMockExtension.newInstance()
            .options(
                    WireMockConfiguration
                            .wireMockConfig()
                            .port(8080)
            )
            .build();

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TransactionDetailsRepository transactionDetailsRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper
            = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    JwtUtils jwtUtils;

    @Test
    void test_When_doPayment_isSuccess() throws Exception {

        PaymentRequest paymentRequest = getMockPaymentRequest();

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.post("/payment")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String paymentId = mvcResult.getResponse().getContentAsString();

        Optional<TransactionDetails> transactionDetails = transactionDetailsRepository.findById(Long.valueOf(paymentId));
        assertTrue(transactionDetails.isPresent());

        TransactionDetails p = transactionDetails.get();

        assertEquals(Long.parseLong(paymentId), p.getId());
        assertEquals(paymentRequest.getPaymentMode().name(), p.getPaymentMode());
        assertEquals(paymentRequest.getAmount(), p.getAmount());
        assertEquals(paymentRequest.getOrderId(), p.getOrderId());
    }

    @Test
    @Disabled
    void test_When_doPayment_WithWrongAccess_thenThrow_403() throws Exception {

        PaymentRequest paymentRequest = getMockPaymentRequest();

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.post("/payments")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andReturn();

    }

    @Test
    void test_When_getOrderDetailsByOrderId_isSuccess() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/payment/order/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String actualResponse = mvcResult.getResponse().getContentAsString();
        TransactionDetails transactionDetails = transactionDetailsRepository.findById(1l).get();
        String expectedResponse = getMockPaymentResponse(transactionDetails);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @Disabled
    void test_When_getOrderDetailsByOrderId_isNotFound() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/payment/order/2")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();
    }


    private PaymentRequest getMockPaymentRequest() {
        return PaymentRequest.builder()
                .amount(500)
                .orderId(1)
                .paymentMode(PaymentMode.CASH)
                .referenceNumber(null)
                .build();

    }

    private String getMockPaymentResponse(TransactionDetails transactionDetails) throws IOException {

        PaymentResponse paymentResponse
                = PaymentResponse.builder()
                .paymentId(transactionDetails.getId())
                .orderId(transactionDetails.getOrderId())
                .paymentDate(transactionDetails.getPaymentDate())
                .paymentMode(PaymentMode.valueOf(transactionDetails.getPaymentMode()))
                .status(transactionDetails.getPaymentStatus())
                .amount(transactionDetails.getAmount())
                .build();
        return objectMapper.writeValueAsString(paymentResponse);
    }

    private String getJWTTokenForRoleUser(){

        var loginRequest = new LoginRequest("User1","user1");

        String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername());

        return jwt;
    }

    private String getJWTTokenForRoleAdmin(){

        var loginRequest = new LoginRequest("","");

        String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername());

        return jwt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class LoginRequest {

        private String username;
        private String password;

    }
}
