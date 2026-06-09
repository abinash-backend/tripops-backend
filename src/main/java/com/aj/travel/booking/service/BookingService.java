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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository packageRepository;
    private final BookingMapper bookingMapper;

    @Caching(evict = {
            @CacheEvict(cacheNames = "packages", key = "'active'"),
            @CacheEvict(cacheNames = "packageById", key = "#request.packageId")
    })
    public BookingResponse createBooking(CreateBookingRequest request) {

        Long currentUserId = getCurrentUserId();

        TravelPackage travelPackage =
                packageRepository.findByIdForUpdate(request.getPackageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (travelPackage.getAvailableCapacity() < request.getGuestCount()) {
            throw new InsufficientCapacityException("Not enough capacity available for this package");
        }

        travelPackage.setAvailableCapacity(travelPackage.getAvailableCapacity() - request.getGuestCount());

        Booking booking = bookingMapper.toEntity(request, currentUserId, travelPackage);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        packageRepository.save(travelPackage);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {

        Long currentUserId = getCurrentUserId();

        return bookingRepository.findByUserId(currentUserId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal) authentication.getPrincipal();

        return principal.getId();
    }
}
