package com.aj.travel.booking.mapper;

import com.aj.travel.booking.domain.Booking;
import com.aj.travel.booking.dto.BookingResponse;
import com.aj.travel.booking.dto.CreateBookingRequest;
import com.aj.travel.packages.domain.TravelPackage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BookingMapper {

    public Booking toEntity(CreateBookingRequest request, Long userId, TravelPackage travelPackage) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setPackageId(travelPackage.getId());
        booking.setGuestCount(request.getGuestCount());
        booking.setTotalPrice(travelPackage.getPrice().multiply(BigDecimal.valueOf(request.getGuestCount())));
        return booking;
    }

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getPackageId(),
                booking.getGuestCount(),
                booking.getTotalPrice(),
                booking.getStatus() != null ? booking.getStatus().name() : null,
                booking.getBookingDate()
        );
    }
}
