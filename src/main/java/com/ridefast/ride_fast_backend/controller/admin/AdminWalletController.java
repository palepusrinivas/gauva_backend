package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.service.WalletService;
import com.ridefast.ride_fast_backend.service.ApiKeyService;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

  private final com.ridefast.ride_fast_backend.service.UserService userService;
  private final com.ridefast.ride_fast_backend.repository.DriverRepository driverRepository;
  private final com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository paymentTransactionRepository;
  private final WalletService walletService;
  private final ApiKeyService apiKeyService;

  @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
  private String frontendUrl;

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

  @PostMapping("/topup")
  public ResponseEntity<com.ridefast.ride_fast_backend.dto.WalletTopUpResponse> initiateTopUp(
      @RequestBody com.ridefast.ride_fast_backend.dto.WalletTopUpRequest request) throws Exception {

    String ownerId;
    String name;
    String email;
    String contact;

    if (request.getOwnerType() == WalletOwnerType.USER) {
      if (request.getOwnerId() == null)
        throw new IllegalArgumentException("Owner ID is required for USER");
      com.ridefast.ride_fast_backend.model.MyUser user = userService.getUserById(request.getOwnerId());
      ownerId = user.getId();
      name = user.getFullName();
      email = user.getEmail();
      contact = user.getPhone();
    } else {
      if (request.getOwnerId() == null)
        throw new IllegalArgumentException("Owner ID is required for DRIVER");
      com.ridefast.ride_fast_backend.model.Driver driver = driverRepository
          .findById(Long.parseLong(request.getOwnerId()))
          .orElseThrow(() -> new com.ridefast.ride_fast_backend.exception.ResourceNotFoundException("Driver", "id",
              Long.parseLong(request.getOwnerId())));
      ownerId = driver.getId().toString();
      name = driver.getName();
      email = driver.getEmail();
      contact = driver.getMobile();
    }

    // Create initial transaction record
    com.ridefast.ride_fast_backend.model.PaymentTransaction tx = com.ridefast.ride_fast_backend.model.PaymentTransaction
        .builder()
        .userId(ownerId) // Storing ownerId in userId field for now, or we should add ownerType to
                         // PaymentTransaction
        .amount(request.getAmount())
        .currency("INR")
        .provider("RAZORPAY")
        .type("WALLET_TOPUP")
        .status("INITIATED")
        .notes("Admin initiated wallet top-up for " + request.getOwnerType())
        .build();

    tx = paymentTransactionRepository.save(tx);

    com.razorpay.RazorpayClient razorpayClient = new com.razorpay.RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());

    org.json.JSONObject paymentLinkRequest = new org.json.JSONObject();
    paymentLinkRequest.put("amount", request.getAmount().multiply(new java.math.BigDecimal("100")).intValue());
    paymentLinkRequest.put("currency", "INR");
    paymentLinkRequest.put("description", "Wallet Top-up (Admin Initiated)");

    org.json.JSONObject notes = new org.json.JSONObject();
    notes.put("type", "wallet_topup");
    notes.put("user_id", ownerId);
    notes.put("owner_type", request.getOwnerType().toString());
    notes.put("transaction_id", tx.getId());
    paymentLinkRequest.put("notes", notes);

    org.json.JSONObject customer = new org.json.JSONObject();
    customer.put("name", name);
    customer.put("contact", contact);
    customer.put("email", email);
    paymentLinkRequest.put("customer", customer);

    paymentLinkRequest.put("callback_url", frontendUrl + "/wallet/payment/success");
    paymentLinkRequest.put("callback_method", "get");

    com.razorpay.PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

    String paymentLinkId = payment.get("id");
    String paymentLinkUrl = payment.get("short_url");

    // Update transaction with payment link details
    tx.setProviderPaymentLinkId(paymentLinkId);
    tx.setStatus("PENDING");
    paymentTransactionRepository.save(tx);

    com.ridefast.ride_fast_backend.dto.WalletTopUpResponse response = com.ridefast.ride_fast_backend.dto.WalletTopUpResponse
        .builder()
        .paymentLinkUrl(paymentLinkUrl)
        .paymentLinkId(paymentLinkId)
        .amount(request.getAmount())
        .transactionId(tx.getId())
        .status("PENDING")
        .build();

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PostMapping("/debit/user/{userId}")
  public ResponseEntity<WalletTransaction> debitUser(@PathVariable String userId,
      @RequestBody DebitRequest req) {
    WalletTransaction tx = walletService.debit(WalletOwnerType.USER, userId, req.getAmount(),
        "ADMIN_DEBIT", null, req.getReason() + (req.getNotes() != null ? " - " + req.getNotes() : ""));
    return new ResponseEntity<>(tx, HttpStatus.CREATED);
  }

  @PostMapping("/debit/driver/{driverId}")
  public ResponseEntity<WalletTransaction> debitDriver(@PathVariable Long driverId,
      @RequestBody DebitRequest req) {
    WalletTransaction tx = walletService.debit(WalletOwnerType.DRIVER, driverId.toString(), req.getAmount(),
        "ADMIN_DEBIT", null, req.getReason() + (req.getNotes() != null ? " - " + req.getNotes() : ""));
    return new ResponseEntity<>(tx, HttpStatus.CREATED);
  }

  // ==================== RAZORPAY TRANSACTIONS ====================

  @GetMapping("/razorpay-transactions")
  public ResponseEntity<Page<PaymentTransaction>> getRazorpayTransactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String type) {
    
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<PaymentTransaction> transactions;
    
    if (status != null && !status.isBlank()) {
      transactions = paymentTransactionRepository.findByStatus(status, pageRequest);
    } else if (type != null && !type.isBlank()) {
      transactions = paymentTransactionRepository.findByType(type, pageRequest);
    } else {
      transactions = paymentTransactionRepository.findAll(pageRequest);
    }
    
    return ResponseEntity.ok(transactions);
  }

  @GetMapping("/razorpay-transactions/{id}")
  public ResponseEntity<PaymentTransaction> getRazorpayTransaction(@PathVariable Long id) {
    return paymentTransactionRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/razorpay-transactions/stats")
  public ResponseEntity<TransactionStats> getTransactionStats() {
    List<PaymentTransaction> all = paymentTransactionRepository.findAll();
    
    long total = all.size();
    long success = all.stream().filter(t -> "SUCCESS".equals(t.getStatus())).count();
    long pending = all.stream().filter(t -> "PENDING".equals(t.getStatus()) || "INITIATED".equals(t.getStatus())).count();
    long failed = all.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
    
    BigDecimal totalAmount = all.stream()
        .filter(t -> "SUCCESS".equals(t.getStatus()))
        .map(PaymentTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    TransactionStats stats = new TransactionStats();
    stats.setTotalTransactions(total);
    stats.setSuccessCount(success);
    stats.setPendingCount(pending);
    stats.setFailedCount(failed);
    stats.setTotalSuccessAmount(totalAmount);
    
    return ResponseEntity.ok(stats);
  }

  @Data
  public static class TransactionStats {
    private long totalTransactions;
    private long successCount;
    private long pendingCount;
    private long failedCount;
    private BigDecimal totalSuccessAmount;
  }

  @Data
  public static class CreditRequest {
    private BigDecimal amount;
    private String notes;
  }

  @Data
  public static class DebitRequest {
    private BigDecimal amount;
    private String reason;
    private String notes;
  }
}
