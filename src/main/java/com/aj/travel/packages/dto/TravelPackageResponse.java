package com.aj.travel.packages.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPackageResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private BigDecimal price;
    private Integer capacity;
    private Integer availableCapacity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
}
