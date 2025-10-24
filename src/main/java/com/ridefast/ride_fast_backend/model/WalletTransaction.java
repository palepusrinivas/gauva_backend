package com.ridefast.ride_fast_backend.model;

import com.ridefast.ride_fast_backend.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Wallet wallet;

  @Enumerated(EnumType.STRING)
  private WalletTransactionType type; // CREDIT or DEBIT

  @Column(nullable = false)
  private BigDecimal amount;

  private String currency = "INR";

  private String referenceType; // RIDE / PAYOUT / TOPUP
  private String referenceId;

  private String status = "SUCCESS"; // PENDING/SUCCESS/FAILED (simple for now)
  private String notes;

  private LocalDateTime createdAt;

  @PrePersist
  public void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
