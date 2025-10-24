package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WalletController {

  private final WalletService walletService;

  @GetMapping("/{ownerType}/{ownerId}")
  public ResponseEntity<BalanceResponse> getBalance(@PathVariable("ownerType") WalletOwnerType ownerType,
                                                    @PathVariable("ownerId") String ownerId) {
    return new ResponseEntity<>(new BalanceResponse(walletService.getBalance(ownerType, ownerId)), HttpStatus.OK);
  }

  @GetMapping("/{ownerType}/{ownerId}/transactions")
  public ResponseEntity<List<WalletTransaction>> getTransactions(@PathVariable("ownerType") WalletOwnerType ownerType,
                                                                 @PathVariable("ownerId") String ownerId) {
    return new ResponseEntity<>(walletService.listTransactions(ownerType, ownerId), HttpStatus.OK);
  }

  @PostMapping("/{ownerType}/{ownerId}/withdraw")
  public ResponseEntity<WalletTransaction> withdraw(@PathVariable("ownerType") WalletOwnerType ownerType,
                                                    @PathVariable("ownerId") String ownerId,
                                                    @RequestBody WithdrawRequest request) {
    WalletTransaction tx = walletService.debit(ownerType, ownerId, request.getAmount(), "PAYOUT", null, request.getNotes());
    return new ResponseEntity<>(tx, HttpStatus.CREATED);
  }

  @Data
  @AllArgsConstructor
  static class BalanceResponse {
    private BigDecimal balance;
  }

  @Data
  static class WithdrawRequest {
    private BigDecimal amount;
    private String notes;
  }
}
