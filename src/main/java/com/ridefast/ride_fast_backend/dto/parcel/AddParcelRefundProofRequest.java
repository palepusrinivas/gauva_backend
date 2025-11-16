package com.ridefast.ride_fast_backend.dto.parcel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddParcelRefundProofRequest {
    @NotBlank
    private String imageUrl;
}


