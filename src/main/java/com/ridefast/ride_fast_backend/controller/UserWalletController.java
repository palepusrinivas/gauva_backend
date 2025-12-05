package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.service.UserService;
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
 * User-facing wallet endpoints - allows users to check their own wallet
 */
@RestController
@RequestMapping("/api/v1/user/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final WalletService walletService;
    private final UserService userService;

    /**
     * Get current user's wallet balance
     * GET /api/v1/user/wallet/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getMyBalance(
            @RequestHeader("Authorization") String jwtToken
    ) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        BigDecimal balance = walletService.getBalance(WalletOwnerType.USER, user.getId());
        
        return new ResponseEntity<>(new WalletBalanceResponse(
                balance,
                "INR",
                user.getId(),
                WalletOwnerType.USER.toString()
        ), HttpStatus.OK);
    }

    /**
     * Get current user's wallet transactions
     * GET /api/v1/user/wallet/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransaction>> getMyTransactions(
            @RequestHeader("Authorization") String jwtToken
    ) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        List<WalletTransaction> transactions = walletService.listTransactions(WalletOwnerType.USER, user.getId());
        
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @Data
    @AllArgsConstructor
    static class WalletBalanceResponse {
        private BigDecimal balance;
        private String currency;
        private String userId;
        private String ownerType;
    }
}

