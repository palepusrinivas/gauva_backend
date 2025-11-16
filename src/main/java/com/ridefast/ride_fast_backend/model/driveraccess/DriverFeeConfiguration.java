package com.ridefast.ride_fast_backend.model.driveraccess;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "driver_fee_configurations", indexes = {
        @Index(name = "idx_driver_fee_vehicle_type", columnList = "vehicleType", unique = true)
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverFeeConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String vehicleType; // bike, auto, car

    @Column(nullable = false)
    private Integer dailyTargetTrips;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal perTripFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumWalletBalance;

    @Column(nullable = false)
    private Integer welcomePeriodDays = 3;

    @Column(nullable = false)
    private Integer maxAllowedCancellations = 1;

    @Column(nullable = false)
    private Boolean isActive = true;
}


