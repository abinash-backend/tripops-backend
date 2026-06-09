package com.aj.travel.packages.service;

import com.aj.travel.booking.repository.BookingRepository;
import com.aj.travel.common.exception.ResourceInUseException;
import com.aj.travel.packages.domain.PackageStatus;
import com.aj.travel.packages.domain.TravelPackage;
import com.aj.travel.packages.dto.CreateTravelPackageRequest;
import com.aj.travel.packages.dto.TravelPackageResponse;
import com.aj.travel.packages.mapper.PackageMapper;
import com.aj.travel.packages.repository.TravelPackageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private TravelPackageRepository packageRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PackageMapper packageMapper;

    @InjectMocks
    private PackageService packageService;

    @Test
    void createPackage_success() {
        // Arrange
        CreateTravelPackageRequest request = buildCreateRequest();

        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setTitle("Goa Trip");

        TravelPackage savedPackage = new TravelPackage();
        savedPackage.setId(1L);
        savedPackage.setTitle("Goa Trip");
        savedPackage.setStatus(PackageStatus.ACTIVE);
        savedPackage.setCreatedAt(LocalDateTime.now());

        TravelPackageResponse expectedResponse = new TravelPackageResponse(
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
                savedPackage.getCreatedAt()
        );

        when(packageMapper.toEntity(request)).thenReturn(travelPackage);
        when(packageRepository.save(travelPackage)).thenReturn(savedPackage);
        when(packageMapper.toResponse(savedPackage)).thenReturn(expectedResponse);

        // Act
        TravelPackageResponse response = packageService.createPackage(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Goa Trip", response.getTitle());
        verify(packageMapper).toEntity(request);
        verify(packageRepository).save(travelPackage);
        verify(packageMapper).toResponse(savedPackage);
    }

    @Test
    void updatePackage_success() {
        // Arrange
        CreateTravelPackageRequest request = buildCreateRequest();

        TravelPackage existingPackage = new TravelPackage();
        existingPackage.setId(1L);
        existingPackage.setTitle("Old Title");

        TravelPackage updatedPackage = new TravelPackage();
        updatedPackage.setId(1L);
        updatedPackage.setTitle("Goa Trip");
        updatedPackage.setStatus(PackageStatus.ACTIVE);

        TravelPackageResponse expectedResponse = new TravelPackageResponse();
        expectedResponse.setId(1L);
        expectedResponse.setTitle("Goa Trip");

        when(packageRepository.findById(1L)).thenReturn(Optional.of(existingPackage));
        when(packageRepository.save(existingPackage)).thenReturn(updatedPackage);
        when(packageMapper.toResponse(updatedPackage)).thenReturn(expectedResponse);

        // Act
        TravelPackageResponse response = packageService.updatePackage(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Goa Trip", response.getTitle());
        verify(packageRepository).findById(1L);
        verify(packageMapper).updateEntity(existingPackage, request);
        verify(packageRepository).save(existingPackage);
        verify(packageMapper).toResponse(updatedPackage);
    }

    @Test
    void deletePackage_success() {
        // Arrange
        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(1L);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));
        when(bookingRepository.existsByPackageId(1L)).thenReturn(false);

        // Act
        packageService.deletePackage(1L);

        // Assert
        verify(packageRepository).findById(1L);
        verify(bookingRepository).existsByPackageId(1L);
        verify(packageRepository).delete(travelPackage);
    }

    @Test
    void deletePackage_withExistingBookings_throwsConflict() {
        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(1L);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));
        when(bookingRepository.existsByPackageId(1L)).thenReturn(true);

        ResourceInUseException exception = assertThrows(
                ResourceInUseException.class,
                () -> packageService.deletePackage(1L)
        );

        assertEquals("Package cannot be deleted because bookings exist", exception.getMessage());
        verify(packageRepository).findById(1L);
        verify(bookingRepository).existsByPackageId(1L);
    }

    @Test
    void getPackages_success() {
        // Arrange
        TravelPackage packageOne = new TravelPackage();
        packageOne.setId(1L);
        TravelPackage packageTwo = new TravelPackage();
        packageTwo.setId(2L);

        TravelPackageResponse responseOne = new TravelPackageResponse();
        responseOne.setId(1L);
        TravelPackageResponse responseTwo = new TravelPackageResponse();
        responseTwo.setId(2L);

        when(packageRepository.findByStatus(PackageStatus.ACTIVE)).thenReturn(List.of(packageOne, packageTwo));
        when(packageMapper.toResponse(packageOne)).thenReturn(responseOne);
        when(packageMapper.toResponse(packageTwo)).thenReturn(responseTwo);

        // Act
        List<TravelPackageResponse> responses = packageService.getActivePackages();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());
        verify(packageRepository).findByStatus(PackageStatus.ACTIVE);
        verify(packageMapper).toResponse(packageOne);
        verify(packageMapper).toResponse(packageTwo);
    }

    private CreateTravelPackageRequest buildCreateRequest() {
        return new CreateTravelPackageRequest(
                "Goa Trip",
                "Beach holiday",
                "Goa",
                BigDecimal.valueOf(15000),
                10,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 15),
                PackageStatus.ACTIVE
        );
    }
}
