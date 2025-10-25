package com.ridefast.ride_fast_backend.model.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "key_name", length = 191)
  private String keyName;

  @Column(name = "live_values", columnDefinition = "TEXT")
  private String liveValues;

  @Column(name = "test_values", columnDefinition = "TEXT")
  private String testValues;

  @Column(name = "settings_type")
  private String settingsType;

  @Column(name = "mode")
  private String mode;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
