package com.ridefast.ride_fast_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTopUpResponse {

    private String paymentLinkUrl;
    private String paymentLinkId;
    private BigDecimal amount;
    private Long transactionId;
    private String status;
}
