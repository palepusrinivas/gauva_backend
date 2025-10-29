package com.ridefast.ride_fast_backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminKpiResponse {
  private long activeCustomers;
  private long activeDrivers;
  private BigDecimal totalEarningsAllTime;
  private BigDecimal totalEarnings30d;
}
