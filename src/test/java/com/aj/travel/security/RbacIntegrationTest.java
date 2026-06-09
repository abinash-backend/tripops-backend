package com.aj.travel.security;

import com.aj.travel.booking.repository.BookingRepository;
import com.aj.travel.packages.domain.PackageStatus;
import com.aj.travel.packages.repository.TravelPackageRepository;
import com.aj.travel.payment.repository.PaymentRepository;
import com.aj.travel.user.domain.User;
import com.aj.travel.user.domain.UserRole;
import com.aj.travel.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        travelPackageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testPublicEndpoints() throws Exception {
        String email = "public-user@test.com";
        String password = "Password@123";

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("TripOps Backend API is running"));

        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest("Public User", email, password, "9876543210"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/packages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(1L, 2))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserAccess() throws Exception {
        String adminToken = createAdminIfNeeded("rbac-admin@test.com", "Admin@123");
        String userToken = createUserIfNeeded("rbac-user@test.com", "Password@123");
        Long packageId = createPackageAsAdmin(adminToken, "Shared Package");

        mockMvc.perform(get("/packages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/packages/{id}", packageId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(packageId));

        Long bookingId = createBookingAsUser(userToken, packageId);

        mockMvc.perform(get("/bookings/my")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest(bookingId, "CARD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        mockMvc.perform(post("/payments/confirm/{bookingId}", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/packages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packageRequest("Forbidden Package"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/register")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(
                                "Blocked Admin",
                                "blocked-admin@test.com",
                                "Admin@123",
                                "9999999991"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminAccess() throws Exception {
        String adminToken = createAdminIfNeeded("root-admin@test.com", "Admin@123");
        String userToken = createUserIfNeeded("bookings-user@test.com", "Password@123");

        mockMvc.perform(post("/admin/register")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(
                                "Second Admin",
                                "second-admin@test.com",
                                "Admin@123",
                                "9999999992"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        Long packageId = createPackageAsAdmin(adminToken, "Admin Package");

        mockMvc.perform(put("/packages/{id}", packageId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packageRequest("Updated Admin Package"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(packageId))
                .andExpect(jsonPath("$.data.title").value("Updated Admin Package"));

        mockMvc.perform(get("/packages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk());

        Long bookingId = createBookingAsUser(userToken, packageId);

        mockMvc.perform(get("/bookings/all")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(packageId, 2))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest(bookingId, "CARD"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/packages/{id}", packageId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Package cannot be deleted because bookings exist"));
    }

    @Test
    void testInvalidTokenAccess() throws Exception {
        mockMvc.perform(get("/packages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/packages")
                        .header(HttpHeaders.AUTHORIZATION, "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private String getToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result).path("data").path("token").asText();
    }

    private String createUserIfNeeded(String email, String password) throws Exception {
        if (!userRepository.existsByEmail(email)) {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest(
                                    "Registered User",
                                    email,
                                    password,
                                    "9876543211"
                            ))))
                    .andExpect(status().isCreated());
        }

        return getToken(email, password);
    }

    private String createAdminIfNeeded(String email, String password) throws Exception {
        if (!userRepository.existsByEmail(email)) {
            User admin = new User();
            admin.setName("Seed Admin");
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setPhone("9999999990");
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }

        return getToken(email, password);
    }

    private Long createPackageAsAdmin(String adminToken, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/packages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packageRequest(title))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result).path("data").path("id").asLong();
    }

    private Long createBookingAsUser(String userToken, Long packageId) throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(packageId, 2))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result).path("data").path("id").asLong();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> userRequest(String name, String email, String password, String phone) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("email", email);
        request.put("password", password);
        request.put("phone", phone);
        return request;
    }

    private Map<String, Object> loginRequest(String email, String password) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("email", email);
        request.put("password", password);
        return request;
    }

    private Map<String, Object> packageRequest(String title) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("description", "Package description");
        request.put("location", "Goa");
        request.put("price", BigDecimal.valueOf(15000));
        request.put("capacity", 10);
        request.put("startDate", LocalDate.of(2026, 5, 10));
        request.put("endDate", LocalDate.of(2026, 5, 15));
        request.put("status", PackageStatus.ACTIVE.name());
        return request;
    }

    private Map<String, Object> bookingRequest(Long packageId, int guestCount) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("packageId", packageId);
        request.put("guestCount", guestCount);
        return request;
    }

    private Map<String, Object> paymentRequest(Long bookingId, String paymentMethod) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("bookingId", bookingId);
        request.put("paymentMethod", paymentMethod);
        return request;
    }
}
