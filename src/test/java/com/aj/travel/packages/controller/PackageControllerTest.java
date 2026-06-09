package com.aj.travel.packages.controller;

import com.aj.travel.auth.security.JwtAuthenticationFilter;
import com.aj.travel.auth.security.UserAuthenticationService;
import com.aj.travel.common.config.SecurityConfig;
import com.aj.travel.packages.dto.TravelPackageResponse;
import com.aj.travel.packages.service.PackageService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PackageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PackageService packageService;

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
    void getPackages_success() throws Exception {

        List<TravelPackageResponse> responses = List.of(
                new TravelPackageResponse(
                        1L,
                        "Goa Trip",
                        "Beach holiday",
                        "Goa",
                        BigDecimal.valueOf(15000),
                        10,
                        10,
                        LocalDate.of(2026, 5, 10),
                        LocalDate.of(2026, 5, 15),
                        "ACTIVE",
                        LocalDateTime.now()
                )
        );

        when(packageService.getActivePackages()).thenReturn(responses);

        mockMvc.perform(get("/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Packages retrieved successfully"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Goa Trip"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        verify(packageService).getActivePackages();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPackage_adminOnly_userForbidden() throws Exception {

        String requestJson = """
                {
                  "title":"Goa Trip",
                  "description":"Beach holiday",
                  "location":"Goa",
                  "price":15000,
                  "capacity":10,
                  "startDate":"2026-05-10",
                  "endDate":"2026-05-15",
                  "status":"ACTIVE"
                }
                """;

        mockMvc.perform(post("/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());

        verify(packageService, never()).createPackage(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPackage_adminAllowed() throws Exception {

        String requestJson = """
                {
                  "title":"Goa Trip",
                  "description":"Beach holiday",
                  "location":"Goa",
                  "price":15000,
                  "capacity":10,
                  "startDate":"2026-05-10",
                  "endDate":"2026-05-15",
                  "status":"ACTIVE"
                }
                """;

        mockMvc.perform(post("/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        verify(packageService).createPackage(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updatePackage_adminOnly_userForbidden() throws Exception {

        String requestJson = """
                {
                  "title":"Updated Goa Trip",
                  "description":"Updated beach holiday",
                  "location":"Goa",
                  "price":17000,
                  "capacity":12,
                  "startDate":"2026-05-12",
                  "endDate":"2026-05-18",
                  "status":"ACTIVE"
                }
                """;

        mockMvc.perform(put("/packages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());

        verify(packageService, never()).updatePackage(any(), any());
    }
}
