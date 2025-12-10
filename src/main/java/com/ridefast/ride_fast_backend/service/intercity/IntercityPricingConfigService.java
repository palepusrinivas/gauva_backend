package com.ridefast.ride_fast_backend.service.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.IntercityPricingConfigRequest;
import com.ridefast.ride_fast_backend.model.intercity.IntercityPricingConfig;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityPricingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for managing intercity pricing configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntercityPricingConfigService {
    
    private final IntercityPricingConfigRepository repository;
    
    /**
     * Get the current pricing configuration
     * Creates default config if none exists
     */
    @Transactional
    public IntercityPricingConfig getOrCreate() {
        IntercityPricingConfig config = repository.findFirstByOrderByIdAsc();
        if (config == null) {
            log.info("No intercity pricing config found, creating default configuration");
            config = createDefaultConfig();
        }
        return config;
    }
    
    /**
     * Update pricing configuration
     */
    @Transactional
    public IntercityPricingConfig update(IntercityPricingConfigRequest request) {
        IntercityPricingConfig config = getOrCreate();
        
        // Update only provided fields
        if (request.getCommissionPercent() != null) {
            config.setCommissionPercent(request.getCommissionPercent());
        }
        if (request.getPlatformFeePercent() != null) {
            config.setPlatformFeePercent(request.getPlatformFeePercent());
        }
        if (request.getGstPercent() != null) {
            config.setGstPercent(request.getGstPercent());
        }
        if (request.getMinCommissionAmount() != null) {
            config.setMinCommissionAmount(request.getMinCommissionAmount());
        }
        if (request.getMaxCommissionAmount() != null) {
            config.setMaxCommissionAmount(request.getMaxCommissionAmount());
        }
        if (request.getNightFareMultiplier() != null) {
            config.setNightFareMultiplier(request.getNightFareMultiplier());
        }
        if (request.getDefaultRoutePriceMultiplier() != null) {
            config.setDefaultRoutePriceMultiplier(request.getDefaultRoutePriceMultiplier());
        }
        if (request.getCommissionEnabled() != null) {
            config.setCommissionEnabled(request.getCommissionEnabled());
        }
        if (request.getNightFareEnabled() != null) {
            config.setNightFareEnabled(request.getNightFareEnabled());
        }
        if (request.getNightFareStartHour() != null) {
            config.setNightFareStartHour(request.getNightFareStartHour());
        }
        if (request.getNightFareEndHour() != null) {
            config.setNightFareEndHour(request.getNightFareEndHour());
        }
        
        IntercityPricingConfig saved = repository.save(config);
        log.info("Intercity pricing configuration updated: commission={}%, nightFareMultiplier={}", 
                saved.getCommissionPercent(), saved.getNightFareMultiplier());
        return saved;
    }
    
    /**
     * Get commission rate as BigDecimal (e.g., 0.05 for 5%)
     */
    @Transactional(readOnly = true)
    public BigDecimal getCommissionRate() {
        IntercityPricingConfig config = getOrCreate();
        if (!Boolean.TRUE.equals(config.getCommissionEnabled())) {
            return BigDecimal.ZERO;
        }
        // Convert percentage to decimal (5.0% -> 0.05)
        return config.getCommissionPercent().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Create default pricing configuration
     */
    private IntercityPricingConfig createDefaultConfig() {
        IntercityPricingConfig config = IntercityPricingConfig.builder()
                .commissionPercent(new BigDecimal("5.00"))
                .nightFareMultiplier(new BigDecimal("1.20"))
                .defaultRoutePriceMultiplier(BigDecimal.ONE)
                .commissionEnabled(true)
                .nightFareEnabled(true)
                .nightFareStartHour(22)
                .nightFareEndHour(6)
                .build();
        return repository.save(config);
    }
}
