package com.ridefast.ride_fast_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminZoneStat {
  private String zone;
  private long trips;
  private long totalDuration; // seconds
  private double avgDuration; // seconds
}
