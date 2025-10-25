package com.ridefast.ride_fast_backend.dto;

import lombok.Data;

@Data
public class LocationUpdate {
  private Double lat;
  private Double lng;
  private Integer heading; // degrees 0-359
  private Double speed;    // m/s or km/h (client-defined)
  private Long ts;         // epoch seconds or millis
}
