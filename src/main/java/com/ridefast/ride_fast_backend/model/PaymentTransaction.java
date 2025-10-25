package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long rideId;
  private String userId;   // MyUser.id (String UUID)
  private Long driverId;   // Driver.id (Long)

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "INR";

  // PROVIDER info (e.g., RAZORPAY)
  @Column(nullable = false)
  private String provider;

  // TYPE: PAYMENT, REFUND
  @Column(nullable = false)
  private String type;

  // PROVIDER REFS: payment id, refund id, link id, etc.
  private String providerPaymentId;
  private String providerRefundId;
  private String providerPaymentLinkId;

  // STATUS: CREATED, PENDING, PAID, FAILED, REFUNDED, PARTIAL_REFUNDED
  @Column(nullable = false)
  private String status;

  private String notes;
  private LocalDateTime createdAt;

  @PrePersist
  public void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
