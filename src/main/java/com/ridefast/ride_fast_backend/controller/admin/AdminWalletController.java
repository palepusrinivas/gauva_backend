package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.service.WalletService;
import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

  private final WalletService walletService;

  @PostMapping("/credit/user/{userId}")
  public ResponseEntity<WalletTransaction> creditUser(@PathVariable String userId,
                                                      @RequestBody CreditRequest req) {
    WalletTransaction tx = walletService.credit(WalletOwnerType.USER, userId, req.getAmount(),
        "ADMIN_TOPUP", null, req.getNotes());
    return new ResponseEntity<>(tx, HttpStatus.CREATED);
  }

  @PostMapping("/credit/driver/{driverId}")
  public ResponseEntity<WalletTransaction> creditDriver(@PathVariable Long driverId,
                                                        @RequestBody CreditRequest req) {
    WalletTransaction tx = walletService.credit(WalletOwnerType.DRIVER, driverId.toString(), req.getAmount(),
        "ADMIN_TOPUP", null, req.getNotes());
    return new ResponseEntity<>(tx, HttpStatus.CREATED);
  }

  @Data
  public static class CreditRequest {
    private BigDecimal amount;
    private String notes;
  }
}
