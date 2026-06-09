package com.aj.travel.booking.mapper;

import com.aj.travel.booking.domain.Booking;
import com.aj.travel.booking.dto.CreateBookingRequest;
import com.aj.travel.packages.domain.TravelPackage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BookingMapperTest {

    private final BookingMapper bookingMapper = new BookingMapper();

    @Test
    void toEntity_calculatesTotalPriceFromGuestCount() {
        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(3L);
        travelPackage.setPrice(BigDecimal.valueOf(15000));

        CreateBookingRequest request = new CreateBookingRequest(3L, 4);

        Booking booking = bookingMapper.toEntity(request, 9L, travelPackage);

        assertEquals(9L, booking.getUserId());
        assertEquals(3L, booking.getPackageId());
        assertEquals(4, booking.getGuestCount());
        assertEquals(BigDecimal.valueOf(60000), booking.getTotalPrice());
    }

    @Test
    void prePersist_populatesBookingDateWhenMissing() {
        Booking booking = new Booking();

        booking.prePersist();

        assertNotNull(booking.getBookingDate());
    }

    @Test
    void prePersist_preservesExistingBookingDate() {
        Booking booking = new Booking();
        LocalDateTime existingDate = LocalDateTime.of(2026, 6, 2, 10, 30);
        booking.setBookingDate(existingDate);

        booking.prePersist();

        assertEquals(existingDate, booking.getBookingDate());
    }
}
