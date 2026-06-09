package com.aj.travel.payment.controller;

import com.aj.travel.auth.security.JwtAuthenticationFilter;
import com.aj.travel.auth.security.UserAuthenticationService;
import com.aj.travel.common.config.SecurityConfig;
import com.aj.travel.payment.dto.PaymentResponse;
import com.aj.travel.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserAuthenticationService userAuthenticationService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPayment_success() throws Exception {

        String requestJson = """
                {
                  "bookingId":1,
                  "paymentMethod":"CARD"
                }
                """;

        PaymentResponse response = new PaymentResponse(
                1L,
                1L,
                BigDecimal.valueOf(2500),
                "CARD",
                null,
                null,
                "CREATED",
                LocalDateTime.now()
        );

        when(paymentService.createPayment(any())).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.bookingId").value(1))
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        verify(paymentService).createPayment(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void confirmPayment_userAllowed() throws Exception {

        mockMvc.perform(post("/payments/confirm/1"))
                .andExpect(status().isOk());

        verify(paymentService).confirmPayment(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirmPayment_userOnly_adminForbidden() throws Exception {

        mockMvc.perform(post("/payments/confirm/1"))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).confirmPayment(any());
    }
}
