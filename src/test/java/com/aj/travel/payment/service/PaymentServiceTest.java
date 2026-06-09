package com.aj.travel.payment.service;

import com.aj.travel.auth.security.AuthenticatedUserPrincipal;
import com.aj.travel.booking.domain.Booking;
import com.aj.travel.booking.domain.BookingStatus;
import com.aj.travel.booking.repository.BookingRepository;
import com.aj.travel.common.exception.DuplicateResourceException;
import com.aj.travel.common.exception.ResourceNotFoundException;
import com.aj.travel.payment.domain.Payment;
import com.aj.travel.payment.domain.PaymentStatus;
import com.aj.travel.payment.dto.CreatePaymentRequest;
import com.aj.travel.payment.dto.PaymentResponse;
import com.aj.travel.payment.mapper.PaymentMapper;
import com.aj.travel.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPayment_success() {
        setAuthenticatedUser(5L);

        CreatePaymentRequest request = new CreatePaymentRequest(1L, "CARD");

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(5L);
        booking.setTotalPrice(BigDecimal.valueOf(2500));

        Payment payment = new Payment();

        Payment savedPayment = new Payment();
        savedPayment.setId(20L);
        savedPayment.setStatus(PaymentStatus.CREATED);

        PaymentResponse expected = new PaymentResponse(
                20L,
                1L,
                BigDecimal.valueOf(2500),
                "CARD",
                null,
                null,
                "CREATED",
                null
        );

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentMapper.toEntity(request, booking)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(expected);

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("CREATED", response.getStatus());
        assertNull(response.getPaidAt());

        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_existingPaymentForBookingThrowsConflict() {
        setAuthenticatedUser(5L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(5L);

        Payment existingPayment = new Payment();
        existingPayment.setId(20L);
        existingPayment.setBookingId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(existingPayment));

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> paymentService.createPayment(new CreatePaymentRequest(1L, "CARD"))
        );

        assertEquals("Payment already exists for this booking", exception.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_bookingNotFound() {
        setAuthenticatedUser(5L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createPayment(new CreatePaymentRequest(100L, "UPI")));
    }

    @Test
    void createPayment_wrongUserOwnership() {
        setAuthenticatedUser(5L);

        Booking booking = new Booking();
        booking.setUserId(99L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(AccessDeniedException.class,
                () -> paymentService.createPayment(new CreatePaymentRequest(1L, "UPI")));
        verify(paymentRepository, never()).findByBookingId(any());
    }

    @Test
    void confirmPayment_success() {
        setAuthenticatedUser(5L); // 🔥 REQUIRED NOW

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(5L);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        Payment payment = new Payment();
        payment.setId(20L);
        payment.setBookingId(1L);
        payment.setStatus(PaymentStatus.CREATED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        paymentService.confirmPayment(1L);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNotNull(payment.getPaidAt());
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmPayment_paymentNotFound() {
        setAuthenticatedUser(5L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(5L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.confirmPayment(1L)
        );

        assertEquals("Payment not found", exception.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmPayment_wrongUser() {
        setAuthenticatedUser(5L);

        Booking booking = new Booking();
        booking.setUserId(99L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(AccessDeniedException.class,
                () -> paymentService.confirmPayment(1L));
        verify(paymentRepository, never()).findByBookingId(any());
    }

    private void setAuthenticatedUser(Long userId) {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        userId,
                        "user@example.com",
                        List.of()
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
