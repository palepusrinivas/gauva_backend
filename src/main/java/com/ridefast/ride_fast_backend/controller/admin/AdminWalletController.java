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

  private final com.ridefast.ride_fast_backend.service.UserService userService;
  private final com.ridefast.ride_fast_backend.repository.DriverRepository driverRepository;
  private final com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository paymentTransactionRepository;
  private final WalletService walletService;

  @org.springframework.beans.factory.annotation.Value("${app.razorpay.key-id}")
  private String razorpayKeyId;

  @org.springframework.beans.factory.annotation.Value("${app.razorpay.key-secret}")
  private String razorpayKeySecret;

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

    com.razorpay.RazorpayClient razorpayClient = new com.razorpay.RazorpayClient(razorpayKeyId, razorpayKeySecret);

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
