package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.dto.cashback.*;
import com.ridefast.ride_fast_backend.enums.CashbackStatus;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.service.UserService;
import com.ridefast.ride_fast_backend.service.cashback.CashbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Customer API for cashback wallet and usage
 */
@RestController
@RequestMapping("/api/v1/cashback")
@RequiredArgsConstructor
@Slf4j
public class CustomerCashbackController {

    private final CashbackService cashbackService;
    private final UserService userService;

    // ==================== WALLET ENDPOINTS ====================

    /**
     * Get user's cashback wallet
     * Shows total balance, active entries with countdown timers
     */
    @GetMapping("/wallet")
    public ResponseEntity<CashbackWalletDTO> getWallet(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        return ResponseEntity.ok(cashbackService.getUserWallet(user.getId()));
    }

    /**
     * Get user's total active balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        CashbackSettingsDTO settings = cashbackService.getSettingsDTO();
        
        return ResponseEntity.ok(Map.of(
                "balance", cashbackService.getActiveBalance(user.getId()),
                "utilisationLimit", settings.getUtilisationLimit(),
                "canUseCashback", cashbackService.canUseCashback(user.getId())
        ));
    }

    /**
     * Check if cashback can be used on next booking
     * Use this on booking screen to show/hide cashback checkbox
     */
    @GetMapping("/can-use")
    public ResponseEntity<Map<String, Object>> canUseCashback(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        CashbackSettingsDTO settings = cashbackService.getSettingsDTO();
        boolean canUse = cashbackService.canUseCashback(user.getId());
        
        return ResponseEntity.ok(Map.of(
                "canUse", canUse,
                "utilisationLimit", settings.getUtilisationLimit(),
                "balance", cashbackService.getActiveBalance(user.getId()),
                "message", canUse 
                        ? String.format("Use ₹%.0f cashback on this trip", settings.getUtilisationLimit())
                        : "Insufficient cashback balance"
        ));
    }

    // ==================== HISTORY ENDPOINTS ====================

    /**
     * Get active cashback entries (with countdown timers)
     */
    @GetMapping("/active")
    public ResponseEntity<List<CashbackEntryDTO>> getActiveEntries(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        return ResponseEntity.ok(cashbackService.getCashbackHistory(user.getId(), CashbackStatus.ACTIVE));
    }

    /**
     * Get used cashback history
     */
    @GetMapping("/history/used")
    public ResponseEntity<List<CashbackEntryDTO>> getUsedHistory(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        return ResponseEntity.ok(cashbackService.getCashbackHistory(user.getId(), CashbackStatus.USED));
    }

    /**
     * Get expired cashback history
     */
    @GetMapping("/history/expired")
    public ResponseEntity<List<CashbackEntryDTO>> getExpiredHistory(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        return ResponseEntity.ok(cashbackService.getCashbackHistory(user.getId(), CashbackStatus.EXPIRED));
    }

    /**
     * Get all cashback history
     */
    @GetMapping("/history")
    public ResponseEntity<List<CashbackEntryDTO>> getAllHistory(
            @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        return ResponseEntity.ok(cashbackService.getCashbackHistory(user.getId(), null));
    }

    // ==================== USAGE ENDPOINTS ====================

    /**
     * Apply cashback to a ride
     * Call this during booking checkout
     */
    @PostMapping("/use")
    public ResponseEntity<CashbackUsageResponse> useCashback(
            @RequestHeader("Authorization") String jwtToken,
            @RequestBody CashbackUsageRequest request) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        log.info("User {} attempting to use cashback for ride {}", user.getId(), request.getRideId());
        return ResponseEntity.ok(cashbackService.useCashback(user.getId(), request));
    }

    /**
     * Calculate fare with cashback applied
     * Use this to show preview on booking screen
     */
    @GetMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateFareWithCashback(
            @RequestHeader("Authorization") String jwtToken,
            @RequestParam java.math.BigDecimal fareAmount) throws ResourceNotFoundException, UserException {
        MyUser user = userService.getRequestedUserProfile(jwtToken);
        CashbackSettingsDTO settings = cashbackService.getSettingsDTO();
        
        boolean canUse = cashbackService.canUseCashback(user.getId());
        java.math.BigDecimal cashbackToUse = canUse ? settings.getUtilisationLimit() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal finalFare = fareAmount.subtract(cashbackToUse);
        
        return ResponseEntity.ok(Map.of(
                "originalFare", fareAmount,
                "canUseCashback", canUse,
                "cashbackAmount", cashbackToUse,
                "finalFare", finalFare.max(java.math.BigDecimal.ZERO),
                "savings", cashbackToUse
        ));
    }

    // ==================== INFO ENDPOINTS ====================

    /**
     * Get current cashback settings (public info for customer)
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCashbackInfo() {
        CashbackSettingsDTO settings = cashbackService.getSettingsDTO();
        
        return ResponseEntity.ok(Map.of(
                "isEnabled", settings.getIsEnabled(),
                "percentage", settings.getEffectivePercentage(),
                "utilisationLimit", settings.getUtilisationLimit(),
                "validityHours", settings.getValidityHours(),
                "isFestivalActive", settings.getIsFestivalActive(),
                "festivalBonus", settings.getFestivalExtraPercentage() != null ? settings.getFestivalExtraPercentage() : 0,
                "categories", settings.getEnabledCategories()
        ));
    }
}

