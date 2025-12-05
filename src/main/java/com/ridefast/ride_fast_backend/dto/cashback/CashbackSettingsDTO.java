package com.ridefast.ride_fast_backend.dto.cashback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackSettingsDTO {
    private Long id;
    private Boolean isEnabled;
    private BigDecimal cashbackPercentage;
    private BigDecimal utilisationLimit;
    private Integer validityHours;
    private Integer maxCreditsPerDay;
    
    // Festival settings
    private BigDecimal festivalExtraPercentage;
    private LocalDateTime festivalStartDate;
    private LocalDateTime festivalEndDate;
    private Boolean isFestivalActive;
    
    // Effective percentage (with festival bonus if active)
    private BigDecimal effectivePercentage;
    
    // Enabled categories
    private List<String> enabledCategories;
    
    private LocalDateTime updatedAt;
}

