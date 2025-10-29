package com.ridefast.ride_fast_backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRecentTrip {
  private Long rideId;
  private String userId;
  private Long driverId;
  private String pickupArea;
  private String destinationArea;
  private Double fare;
  private Long duration;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
}
