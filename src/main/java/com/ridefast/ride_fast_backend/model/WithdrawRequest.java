package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdraw_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "withdraw_method_id", nullable = false)
    private WithdrawMethod withdrawMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO; // amount - processingFee

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, SETTLED, DENIED

    @Column(columnDefinition = "TEXT")
    private String accountDetails; // JSON with bank account/UPI details

    @Column(length = 500)
    private String adminNote; // Note from admin when approving/denying

    @Column(length = 500)
    private String driverNote; // Note from driver when requesting

    @Column(length = 100)
    private String transactionId; // External transaction ID after settlement

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime settledAt;

    @Column
    private LocalDateTime deniedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (netAmount == null || netAmount.compareTo(BigDecimal.ZERO) == 0) {
            netAmount = amount.subtract(processingFee != null ? processingFee : BigDecimal.ZERO);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

