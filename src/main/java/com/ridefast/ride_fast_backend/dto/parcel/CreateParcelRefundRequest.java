package com.ridefast.ride_fast_backend.dto.parcel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateParcelRefundRequest {
    @NotNull
    private Long rideId;
    @NotNull
    private Long userId;
    @NotBlank
    private String reason;
}


