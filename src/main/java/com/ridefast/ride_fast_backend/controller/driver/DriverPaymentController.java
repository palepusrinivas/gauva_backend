package com.ridefast.ride_fast_backend.controller.driver;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.ridefast.ride_fast_backend.dto.WalletTopUpRequest;
import com.ridefast.ride_fast_backend.dto.WalletTopUpResponse;
import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.model.Wallet;
import com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.WalletService;
import com.ridefast.ride_fast_backend.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Driver payment endpoints for wallet top-up via Razorpay
 */
@RestController
@RequestMapping("/api/v1/driver/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
@Slf4j
public class DriverPaymentController {

    private final DriverService driverService;
    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApiKeyService apiKeyService;

    @Value("${app.wallet.topup.min-amount:10}")
    private int minTopUpAmount;

    @Value("${app.wallet.topup.max-amount:50000}")
    private int maxTopUpAmount;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Create wallet top-up payment link for driver
     * POST /api/v1/driver/payments/wallet/topup
     */
    @PostMapping("/wallet/topup")
    public ResponseEntity<?> createWalletTopUp(
            @Valid @RequestBody WalletTopUpRequest request,
            @RequestHeader("Authorization") String jwtToken) {

        try {
            if (request.getAmount().intValue() < minTopUpAmount || request.getAmount().intValue() > maxTopUpAmount) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Amount must be between " + minTopUpAmount + " and " + maxTopUpAmount
                ));
            }

            Driver driver = driverService.getRequestedDriverProfile(jwtToken);

            // Create initial transaction record
            PaymentTransaction tx = PaymentTransaction.builder()
                    .driverId(driver.getId())
                    .amount(request.getAmount())
                    .currency("INR")
                    .provider("RAZORPAY")
                    .type("WALLET_TOPUP")
                    .status("INITIATED")
                    .notes("Driver wallet top-up initiated")
                    .build();

            tx = paymentTransactionRepository.save(tx);

            RazorpayClient razorpayClient = new RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());

            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
            paymentLinkRequest.put("currency", "INR");
            paymentLinkRequest.put("description", "Driver Wallet Top-up");

            JSONObject notes = new JSONObject();
            notes.put("type", "wallet_topup");
            notes.put("driver_id", driver.getId().toString());
            notes.put("owner_type", "DRIVER");
            notes.put("transaction_id", tx.getId());
            paymentLinkRequest.put("notes", notes);

            JSONObject customer = new JSONObject();
            customer.put("name", driver.getName() != null ? driver.getName() : "Driver");
            customer.put("contact", driver.getMobile() != null ? driver.getMobile() : "");
            customer.put("email", driver.getEmail() != null ? driver.getEmail() : "");
            paymentLinkRequest.put("customer", customer);

            paymentLinkRequest.put("callback_url", frontendUrl + "/driver/wallet/payment/success");
            paymentLinkRequest.put("callback_method", "get");

            PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

            String paymentLinkId = payment.get("id");
            String paymentLinkUrl = payment.get("short_url");

            // Update transaction with payment link details
            tx.setProviderPaymentLinkId(paymentLinkId);
            tx.setStatus("PENDING");
            paymentTransactionRepository.save(tx);

            WalletTopUpResponse response = WalletTopUpResponse.builder()
                    .paymentLinkUrl(paymentLinkUrl)
                    .paymentLinkId(paymentLinkId)
                    .amount(request.getAmount())
                    .transactionId(tx.getId())
                    .status("PENDING")
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error creating driver wallet top-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment: " + e.getMessage()));
        }
    }

    /**
     * Verify payment status
     * GET /api/v1/driver/payments/wallet/verify/{transactionId}
     */
    @GetMapping("/wallet/verify/{transactionId}")
    public ResponseEntity<?> verifyPayment(@PathVariable Long transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .map(tx -> ResponseEntity.ok(Map.of(
                        "transactionId", tx.getId(),
                        "status", tx.getStatus(),
                        "amount", tx.getAmount(),
                        "paymentId", tx.getProviderPaymentId() != null ? tx.getProviderPaymentId() : "",
                        "createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get driver's wallet balance
     * GET /api/v1/driver/payments/wallet/balance
     */
    @GetMapping("/wallet/balance")
    public ResponseEntity<?> getWalletBalance(@RequestHeader("Authorization") String jwtToken) {
        try {
            Driver driver = driverService.getRequestedDriverProfile(jwtToken);
            Wallet wallet = walletService.getOrCreate(WalletOwnerType.DRIVER, driver.getId().toString());

            return ResponseEntity.ok(Map.of(
                    "balance", wallet.getBalance(),
                    "currency", "INR",
                    "driverId", driver.getId()
            ));
        } catch (Exception e) {
            log.error("Error fetching driver wallet balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch balance"));
        }
    }

    /**
     * Get driver's payment transactions
     * GET /api/v1/driver/payments/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestHeader("Authorization") String jwtToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Driver driver = driverService.getRequestedDriverProfile(jwtToken);
            var transactions = paymentTransactionRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching driver transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions"));
        }
    }
}

