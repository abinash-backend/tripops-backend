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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;

    public PaymentResponse createPayment(CreatePaymentRequest request) {

        Long currentUserId = getCurrentUserId();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        validateBookingOwnership(booking, currentUserId);

        if (paymentRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new DuplicateResourceException("Payment already exists for this booking");
        }

        Payment payment = paymentMapper.toEntity(request, booking);
        payment.setStatus(PaymentStatus.CREATED);

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    public void confirmPayment(Long bookingId) {

        Long currentUserId = getCurrentUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        validateBookingOwnership(booking, currentUserId);

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private void validateBookingOwnership(Booking booking, Long currentUserId) {
        if (!currentUserId.equals(booking.getUserId())) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("Unauthorized access");
        }

        return principal.getId();
    }
}
