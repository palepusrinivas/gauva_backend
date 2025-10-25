package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "driver_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private MyUser user;

  @Column(name="driver-id")
  private String driverId;

  @Column(name = "is_online")
  private String isOnline; // DDL uses VARCHAR default 'false'

  @Column(name = "availability_status")
  private String availabilityStatus; // default 'unavailable'

  @Column(name = "online")
  private LocalTime online;

  @Column(name = "offline")
  private LocalTime offline;

  @Column(name = "online_time")
  private Double onlineTime; // DOUBLE(23,2)

  @Column(name = "accepted")
  private LocalTime accepted;

  @Column(name = "completed")
  private LocalTime completed;

  @Column(name = "start_driving")
  private LocalTime startDriving;

  @Column(name = "on_driving_time")
  private Double onDrivingTime; // DOUBLE(23,2)

  @Column(name = "idle_time")
  private Double idleTime; // DOUBLE(23,2)

  @Column(name = "ride_count")
  private Integer rideCount; // INT

  @Column(name = "parcel_count")
  private Integer parcelCount; // INT

  @Column(name = "service")
  private String service;

  @Column(name = "created_at")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;
}
