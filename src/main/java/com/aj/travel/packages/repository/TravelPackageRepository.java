package com.aj.travel.packages.repository;

import com.aj.travel.packages.domain.TravelPackage;
import com.aj.travel.packages.domain.PackageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {

    List<TravelPackage> findByStatus(PackageStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TravelPackage p where p.id = :id")
    Optional<TravelPackage> findByIdForUpdate(@Param("id") Long id);

}
