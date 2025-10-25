package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(nullable = false, length = 4096)
  private String value;

  @Column
  private String description;

  @Column
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
