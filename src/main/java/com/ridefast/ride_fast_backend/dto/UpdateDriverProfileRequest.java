package com.ridefast.ride_fast_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverProfileRequest {
    private String name;
    private String email;
    private String mobile;
}

