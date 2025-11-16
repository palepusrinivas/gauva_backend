package com.ridefast.ride_fast_backend.model.driveraccess;

import com.ridefast.ride_fast_backend.model.Driver;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_daily_activities", uniqueConstraints = {
        @UniqueConstraint(name = "uniq_driver_date", columnNames = {"driver_id", "activityDate"})
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDailyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Driver driver;

    @Column(nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false, length = 24)
    private String vehicleType;

    private Integer totalTrips = 0;
    private Integer completedTrips = 0;
    private Integer customerCancelledAfterStart = 0;
    private Integer driverCancelled = 0;

    private Integer targetTrips = 0;
    private BigDecimal dailyFee = BigDecimal.ZERO;

    private Boolean feeDeducted = false;
    private Boolean freeAccessAchieved = false;
    private BigDecimal feeAmountDeducted;
    private LocalDateTime feeDeductedAt;

    private Integer daysSinceJoining = 0;
    private Boolean isWelcomePeriod = false;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


