package com.aj.travel.booking.service;

import com.aj.travel.auth.security.AuthenticatedUserPrincipal;
import com.aj.travel.booking.domain.Booking;
import com.aj.travel.booking.domain.BookingStatus;
import com.aj.travel.booking.dto.BookingResponse;
import com.aj.travel.booking.dto.CreateBookingRequest;
import com.aj.travel.booking.mapper.BookingMapper;
import com.aj.travel.booking.repository.BookingRepository;
import com.aj.travel.common.exception.InsufficientCapacityException;
import com.aj.travel.common.exception.ResourceNotFoundException;
import com.aj.travel.packages.domain.TravelPackage;
import com.aj.travel.packages.repository.TravelPackageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelPackageRepository packageRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBooking_success() {
        setAuthenticatedUser(7L);

        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(1L);
        travelPackage.setPrice(BigDecimal.valueOf(1000));
        travelPackage.setAvailableCapacity(5);

        CreateBookingRequest request = new CreateBookingRequest(1L, 2);

        Booking booking = new Booking();
        booking.setUserId(7L);

        Booking savedBooking = new Booking();
        savedBooking.setId(10L);
        savedBooking.setUserId(7L);
        savedBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        savedBooking.setBookingDate(LocalDateTime.now());

        BookingResponse expectedResponse = new BookingResponse(
                10L, 7L, 1L, 2,
                BigDecimal.valueOf(2000),
                "PENDING_PAYMENT",
                savedBooking.getBookingDate()
        );

        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(travelPackage));
        when(bookingMapper.toEntity(request, 7L, travelPackage)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("PENDING_PAYMENT", response.getStatus());
        assertEquals(3, travelPackage.getAvailableCapacity());

        verify(packageRepository).findByIdForUpdate(1L);
        verify(packageRepository).save(travelPackage);
        verify(bookingMapper).toEntity(request, 7L, travelPackage);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_packageNotFound() {
        setAuthenticatedUser(7L);

        CreateBookingRequest request = new CreateBookingRequest(99L, 2);
        when(packageRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals("Package not found", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_insufficientCapacity() {
        setAuthenticatedUser(7L);

        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(1L);
        travelPackage.setAvailableCapacity(1);

        CreateBookingRequest request = new CreateBookingRequest(1L, 2);

        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(travelPackage));

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals("Not enough capacity available for this package", exception.getMessage());
        assertEquals(1, travelPackage.getAvailableCapacity());
        verify(packageRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getMyBookings_success() {
        setAuthenticatedUser(7L);

        Booking booking = new Booking();
        booking.setId(1L);

        BookingResponse response = new BookingResponse();
        response.setId(1L);

        when(bookingRepository.findByUserId(7L)).thenReturn(List.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        List<BookingResponse> result = bookingService.getMyBookings();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
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
