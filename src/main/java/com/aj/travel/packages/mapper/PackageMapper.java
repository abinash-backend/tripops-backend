package com.aj.travel.packages.mapper;

import com.aj.travel.packages.domain.TravelPackage;
import com.aj.travel.packages.dto.CreateTravelPackageRequest;
import com.aj.travel.packages.dto.TravelPackageResponse;
import org.springframework.stereotype.Component;

@Component
public class PackageMapper {

    public TravelPackage toEntity(CreateTravelPackageRequest request) {
        TravelPackage travelPackage = new TravelPackage();
        updateEntity(travelPackage, request);
        travelPackage.setAvailableCapacity(request.getCapacity());
        return travelPackage;
    }

    public void updateEntity(TravelPackage travelPackage, CreateTravelPackageRequest request) {
        travelPackage.setTitle(request.getTitle());
        travelPackage.setDescription(request.getDescription());
        travelPackage.setLocation(request.getLocation());
        travelPackage.setPrice(request.getPrice());
        travelPackage.setCapacity(request.getCapacity());
        travelPackage.setStartDate(request.getStartDate());
        travelPackage.setEndDate(request.getEndDate());
        travelPackage.setStatus(request.getStatus());
    }

    public TravelPackageResponse toResponse(TravelPackage travelPackage) {
        return new TravelPackageResponse(
                travelPackage.getId(),
                travelPackage.getTitle(),
                travelPackage.getDescription(),
                travelPackage.getLocation(),
                travelPackage.getPrice(),
                travelPackage.getCapacity(),
                travelPackage.getAvailableCapacity(),
                travelPackage.getStartDate(),
                travelPackage.getEndDate(),
                travelPackage.getStatus() != null ? travelPackage.getStatus().name() : null,
                travelPackage.getCreatedAt()
        );
    }
}
