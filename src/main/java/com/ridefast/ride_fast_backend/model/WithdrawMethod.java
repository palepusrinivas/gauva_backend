package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdraw_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Bank Transfer", "UPI", "PayPal"

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String icon; // Icon name or emoji

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Double minimumAmount = 100.0;

    @Column(nullable = false)
    @Builder.Default
    private Double maximumAmount = 50000.0;

    @Column
    @Builder.Default
    private Double processingFee = 0.0; // Fixed fee

    @Column
    @Builder.Default
    private Double processingFeePercent = 0.0; // Percentage fee

    @Column
    @Builder.Default
    private Integer processingDays = 3; // Days to process

    @Column(columnDefinition = "TEXT")
    private String requiredFields; // JSON array of required fields like ["account_number", "ifsc_code"]

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

