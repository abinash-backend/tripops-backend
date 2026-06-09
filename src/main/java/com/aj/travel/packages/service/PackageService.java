package com.aj.travel.packages.service;

import com.aj.travel.booking.repository.BookingRepository;
import com.aj.travel.common.exception.ResourceNotFoundException;
import com.aj.travel.common.exception.ResourceInUseException;
import com.aj.travel.packages.domain.PackageStatus;
import com.aj.travel.packages.domain.TravelPackage;
import com.aj.travel.packages.dto.CreateTravelPackageRequest;
import com.aj.travel.packages.dto.TravelPackageResponse;
import com.aj.travel.packages.mapper.PackageMapper;
import com.aj.travel.packages.repository.TravelPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PackageService {

    private final TravelPackageRepository packageRepository;
    private final BookingRepository bookingRepository;
    private final PackageMapper packageMapper;

    @CacheEvict(cacheNames = "packages", key = "'active'")
    public TravelPackageResponse createPackage(CreateTravelPackageRequest request) {
        TravelPackage travelPackage = packageMapper.toEntity(request);

        return packageMapper.toResponse(packageRepository.save(travelPackage));
    }

    @Caching(
            put = @CachePut(cacheNames = "packageById", key = "#id"),
            evict = @CacheEvict(cacheNames = "packages", key = "'active'")
    )
    public TravelPackageResponse updatePackage(Long id, CreateTravelPackageRequest request) {
        TravelPackage travelPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        packageMapper.updateEntity(travelPackage, request);

        return packageMapper.toResponse(packageRepository.save(travelPackage));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "packages", key = "'active'"),
            @CacheEvict(cacheNames = "packageById", key = "#id")
    })
    public void deletePackage(Long id) {
        TravelPackage travelPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (bookingRepository.existsByPackageId(id)) {
            throw new ResourceInUseException("Package cannot be deleted because bookings exist");
        }

        packageRepository.delete(travelPackage);
    }

    @Cacheable(cacheNames = "packages", key = "'active'")
    @Transactional(readOnly = true)
    public List<TravelPackageResponse> getActivePackages() {
        return packageRepository.findByStatus(PackageStatus.ACTIVE)
                .stream()
                .map(packageMapper::toResponse)
                .toList();
    }

    @Cacheable(cacheNames = "packageById", key = "#id")
    @Transactional(readOnly = true)
    public TravelPackageResponse getPackage(Long id) {
        TravelPackage travelPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        return packageMapper.toResponse(travelPackage);
    }
}
