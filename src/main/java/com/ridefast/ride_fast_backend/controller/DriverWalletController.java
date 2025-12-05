package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Driver-facing wallet endpoints - allows drivers to check their own wallet
 */
@RestController
@RequestMapping("/api/v1/driver/wallet")
@RequiredArgsConstructor
public class DriverWalletController {

    private final WalletService walletService;
    private final DriverService driverService;

    /**
     * Get current driver's wallet balance
     * GET /api/v1/driver/wallet/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getMyBalance(
            @RequestHeader("Authorization") String jwtToken
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        BigDecimal balance = walletService.getBalance(WalletOwnerType.DRIVER, driver.getId().toString());
        
        return new ResponseEntity<>(new WalletBalanceResponse(
                balance,
                "INR",
                driver.getId().toString(),
                WalletOwnerType.DRIVER.toString()
        ), HttpStatus.OK);
    }

    /**
     * Get current driver's wallet transactions
     * GET /api/v1/driver/wallet/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransaction>> getMyTransactions(
            @RequestHeader("Authorization") String jwtToken
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        List<WalletTransaction> transactions = walletService.listTransactions(WalletOwnerType.DRIVER, driver.getId().toString());
        
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @Data
    @AllArgsConstructor
    static class WalletBalanceResponse {
        private BigDecimal balance;
        private String currency;
        private String driverId;
        private String ownerType;
    }
}

