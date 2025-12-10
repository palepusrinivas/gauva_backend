package com.ridefast.ride_fast_backend.dto.intercity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating intercity pricing configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntercityPricingConfigRequest {
    
    @DecimalMin(value = "0.0", message = "Commission percentage must be >= 0")
    @DecimalMax(value = "100.0", message = "Commission percentage must be <= 100")
    private BigDecimal commissionPercent;
    
    @DecimalMin(value = "0.0", message = "Platform fee percentage must be >= 0")
    @DecimalMax(value = "100.0", message = "Platform fee percentage must be <= 100")
    private BigDecimal platformFeePercent;
    
    @DecimalMin(value = "0.0", message = "GST percentage must be >= 0")
    @DecimalMax(value = "100.0", message = "GST percentage must be <= 100")
    private BigDecimal gstPercent;
    
    @DecimalMin(value = "0.0", message = "Minimum commission amount must be >= 0")
    private BigDecimal minCommissionAmount;
    
    @DecimalMin(value = "0.0", message = "Maximum commission amount must be >= 0")
    private BigDecimal maxCommissionAmount;
    
    @DecimalMin(value = "1.0", message = "Night fare multiplier must be >= 1.0")
    @DecimalMax(value = "3.0", message = "Night fare multiplier must be <= 3.0")
    private BigDecimal nightFareMultiplier;
    
    @DecimalMin(value = "0.1", message = "Default route price multiplier must be >= 0.1")
    @DecimalMax(value = "10.0", message = "Default route price multiplier must be <= 10.0")
    private BigDecimal defaultRoutePriceMultiplier;
    
    private Boolean commissionEnabled;
    
    private Boolean nightFareEnabled;
    
    @Min(value = 0, message = "Night fare start hour must be between 0 and 23")
    @Max(value = 23, message = "Night fare start hour must be between 0 and 23")
    private Integer nightFareStartHour;
    
    @Min(value = 0, message = "Night fare end hour must be between 0 and 23")
    @Max(value = 23, message = "Night fare end hour must be between 0 and 23")
    private Integer nightFareEndHour;
}
