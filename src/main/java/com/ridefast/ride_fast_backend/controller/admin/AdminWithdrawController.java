package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.WithdrawMethod;
import com.ridefast.ride_fast_backend.model.WithdrawRequest;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.WithdrawMethodRepository;
import com.ridefast.ride_fast_backend.repository.WithdrawRequestRepository;
import com.ridefast.ride_fast_backend.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminWithdrawController {

    private final WithdrawMethodRepository withdrawMethodRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final DriverRepository driverRepository;
    private final WalletService walletService;

    // ==================== WITHDRAW METHODS ====================

    @GetMapping("/withdraw-method")
    public ResponseEntity<List<WithdrawMethod>> getAllWithdrawMethods() {
        return ResponseEntity.ok(withdrawMethodRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/withdraw-method/active")
    public ResponseEntity<List<WithdrawMethod>> getActiveWithdrawMethods() {
        return ResponseEntity.ok(withdrawMethodRepository.findByActiveTrue());
    }

    @GetMapping("/withdraw-method/{id}")
    public ResponseEntity<WithdrawMethod> getWithdrawMethodById(@PathVariable Long id) {
        return withdrawMethodRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/withdraw-method")
    public ResponseEntity<WithdrawMethod> createWithdrawMethod(@RequestBody WithdrawMethod method) {
        method.setId(null);
        WithdrawMethod saved = withdrawMethodRepository.save(method);
        log.info("Withdraw method created: id={}, name={}", saved.getId(), saved.getName());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/withdraw-method/{id}")
    public ResponseEntity<WithdrawMethod> updateWithdrawMethod(@PathVariable Long id, @RequestBody WithdrawMethod method) {
        return withdrawMethodRepository.findById(id)
                .map(existing -> {
                    existing.setName(method.getName());
                    existing.setDescription(method.getDescription());
                    existing.setIcon(method.getIcon());
                    existing.setActive(method.getActive());
                    existing.setMinimumAmount(method.getMinimumAmount());
                    existing.setMaximumAmount(method.getMaximumAmount());
                    existing.setProcessingFee(method.getProcessingFee());
                    existing.setProcessingFeePercent(method.getProcessingFeePercent());
                    existing.setProcessingDays(method.getProcessingDays());
                    existing.setRequiredFields(method.getRequiredFields());
                    return ResponseEntity.ok(withdrawMethodRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/withdraw-method/{id}")
    public ResponseEntity<Void> deleteWithdrawMethod(@PathVariable Long id) {
        if (!withdrawMethodRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        withdrawMethodRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/withdraw-method/{id}/status")
    public ResponseEntity<WithdrawMethod> toggleWithdrawMethodStatus(@PathVariable Long id, @RequestParam Boolean active) {
        return withdrawMethodRepository.findById(id)
                .map(method -> {
                    method.setActive(active);
                    return ResponseEntity.ok(withdrawMethodRepository.save(method));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== WITHDRAW REQUESTS ====================

    @GetMapping("/withdraw/requests")
    public ResponseEntity<Page<WithdrawRequest>> getAllWithdrawRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawRequest> requests;

        if (search != null && !search.trim().isEmpty()) {
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
                requests = withdrawRequestRepository.searchByStatusAndDriverNameOrMobile(status.toUpperCase(), search.trim(), pageable);
            } else {
                requests = withdrawRequestRepository.searchByDriverNameOrMobile(search.trim(), pageable);
            }
        } else if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            requests = withdrawRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            requests = withdrawRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/withdraw/requests/{id}")
    public ResponseEntity<WithdrawRequest> getWithdrawRequestById(@PathVariable Long id) {
        return withdrawRequestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/withdraw/requests/{id}/approve")
    public ResponseEntity<?> approveWithdrawRequest(@PathVariable Long id, @RequestBody(required = false) AdminNoteRequest request) {
        return withdrawRequestRepository.findById(id)
                .map(withdrawRequest -> {
                    if (!"PENDING".equals(withdrawRequest.getStatus())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Can only approve pending requests"));
                    }
                    
                    withdrawRequest.setStatus("APPROVED");
                    withdrawRequest.setApprovedAt(LocalDateTime.now());
                    if (request != null && request.getNote() != null) {
                        withdrawRequest.setAdminNote(request.getNote());
                    }
                    
                    WithdrawRequest saved = withdrawRequestRepository.save(withdrawRequest);
                    log.info("Withdraw request approved: id={}, driverId={}, amount={}", 
                            saved.getId(), saved.getDriver().getId(), saved.getAmount());
                    
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/withdraw/requests/{id}/settle")
    public ResponseEntity<?> settleWithdrawRequest(@PathVariable Long id, @RequestBody SettleRequest request) {
        return withdrawRequestRepository.findById(id)
                .map(withdrawRequest -> {
                    if (!"APPROVED".equals(withdrawRequest.getStatus())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Can only settle approved requests"));
                    }
                    
                    // Debit from driver wallet
                    try {
                        walletService.debit(
                                WalletOwnerType.DRIVER,
                                withdrawRequest.getDriver().getId().toString(),
                                withdrawRequest.getAmount(),
                                "WITHDRAW",
                                withdrawRequest.getId().toString(),
                                "Withdrawal settled"
                        );
                    } catch (Exception e) {
                        log.error("Failed to debit wallet for withdraw request: {}", id, e);
                        return ResponseEntity.badRequest().body(Map.of("error", "Failed to debit wallet: " + e.getMessage()));
                    }
                    
                    withdrawRequest.setStatus("SETTLED");
                    withdrawRequest.setSettledAt(LocalDateTime.now());
                    if (request.getTransactionId() != null) {
                        withdrawRequest.setTransactionId(request.getTransactionId());
                    }
                    if (request.getNote() != null) {
                        withdrawRequest.setAdminNote(request.getNote());
                    }
                    
                    WithdrawRequest saved = withdrawRequestRepository.save(withdrawRequest);
                    log.info("Withdraw request settled: id={}, driverId={}, amount={}, txnId={}", 
                            saved.getId(), saved.getDriver().getId(), saved.getAmount(), saved.getTransactionId());
                    
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/withdraw/requests/{id}/deny")
    public ResponseEntity<?> denyWithdrawRequest(@PathVariable Long id, @RequestBody AdminNoteRequest request) {
        return withdrawRequestRepository.findById(id)
                .map(withdrawRequest -> {
                    if (!"PENDING".equals(withdrawRequest.getStatus()) && !"APPROVED".equals(withdrawRequest.getStatus())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Can only deny pending or approved requests"));
                    }
                    
                    withdrawRequest.setStatus("DENIED");
                    withdrawRequest.setDeniedAt(LocalDateTime.now());
                    if (request != null && request.getNote() != null) {
                        withdrawRequest.setAdminNote(request.getNote());
                    }
                    
                    WithdrawRequest saved = withdrawRequestRepository.save(withdrawRequest);
                    log.info("Withdraw request denied: id={}, driverId={}, amount={}", 
                            saved.getId(), saved.getDriver().getId(), saved.getAmount());
                    
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/withdraw/stats")
    public ResponseEntity<Map<String, Object>> getWithdrawStats() {
        long total = withdrawRequestRepository.count();
        long pending = withdrawRequestRepository.countByStatus("PENDING");
        long approved = withdrawRequestRepository.countByStatus("APPROVED");
        long settled = withdrawRequestRepository.countByStatus("SETTLED");
        long denied = withdrawRequestRepository.countByStatus("DENIED");
        BigDecimal settledAmount = withdrawRequestRepository.sumSettledAmount();
        BigDecimal pendingAmount = withdrawRequestRepository.sumPendingAmount();

        return ResponseEntity.ok(Map.of(
                "total", total,
                "pending", pending,
                "approved", approved,
                "settled", settled,
                "denied", denied,
                "settledAmount", settledAmount != null ? settledAmount : BigDecimal.ZERO,
                "pendingAmount", pendingAmount != null ? pendingAmount : BigDecimal.ZERO
        ));
    }

    @PostMapping("/withdraw/seed-methods")
    public ResponseEntity<List<WithdrawMethod>> seedDefaultMethods() {
        List<WithdrawMethod> defaults = List.of(
                WithdrawMethod.builder()
                        .name("Bank Transfer")
                        .description("Transfer to your bank account via NEFT/IMPS")
                        .icon("🏦")
                        .active(true)
                        .minimumAmount(100.0)
                        .maximumAmount(50000.0)
                        .processingFee(0.0)
                        .processingFeePercent(0.0)
                        .processingDays(2)
                        .requiredFields("[\"account_number\", \"ifsc_code\", \"account_holder_name\"]")
                        .build(),
                WithdrawMethod.builder()
                        .name("UPI")
                        .description("Transfer to your UPI ID")
                        .icon("📱")
                        .active(true)
                        .minimumAmount(10.0)
                        .maximumAmount(10000.0)
                        .processingFee(0.0)
                        .processingFeePercent(0.0)
                        .processingDays(1)
                        .requiredFields("[\"upi_id\"]")
                        .build(),
                WithdrawMethod.builder()
                        .name("PayTM Wallet")
                        .description("Transfer to your PayTM wallet")
                        .icon("💳")
                        .active(true)
                        .minimumAmount(10.0)
                        .maximumAmount(10000.0)
                        .processingFee(0.0)
                        .processingFeePercent(1.0)
                        .processingDays(1)
                        .requiredFields("[\"paytm_number\"]")
                        .build()
        );

        List<WithdrawMethod> saved = defaults.stream()
                .filter(m -> withdrawMethodRepository.findAllByOrderByNameAsc().stream()
                        .noneMatch(existing -> existing.getName().equals(m.getName())))
                .map(withdrawMethodRepository::save)
                .toList();

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @Data
    public static class AdminNoteRequest {
        private String note;
    }

    @Data
    public static class SettleRequest {
        private String transactionId;
        private String note;
    }
}

