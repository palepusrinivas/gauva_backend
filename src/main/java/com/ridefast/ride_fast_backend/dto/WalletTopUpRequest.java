package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopUpRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private BigDecimal amount;

    @NotNull(message = "Owner type is required")
    private WalletOwnerType ownerType;
}
