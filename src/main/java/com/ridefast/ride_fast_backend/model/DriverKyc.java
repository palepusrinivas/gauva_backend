package com.ridefast.ride_fast_backend.model;

import com.ridefast.ride_fast_backend.enums.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "driver_kyc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverKyc {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "driver_id", unique = true)
  private Driver driver;

  @Column(length = 32)
  private String aadhaarNumber;

  @Column(length = 32)
  private String licenseNumber;

  @Column(length = 32)
  private String rcNumber;

  private String photoKey;
  private String aadhaarFrontKey;
  private String aadhaarBackKey;
  private String licenseFrontKey;
  private String licenseBackKey;
  private String rcFrontKey;
  private String rcBackKey;

  @Enumerated(EnumType.STRING)
  private KycStatus status;

  private String rejectionReason;

  private Instant submittedAt;
  private Instant reviewedAt;
}
