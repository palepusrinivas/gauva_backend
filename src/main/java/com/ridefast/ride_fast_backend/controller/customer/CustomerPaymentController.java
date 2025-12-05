package com.ridefast.ride_fast_backend.controller.customer;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.ridefast.ride_fast_backend.dto.WalletTopUpRequest;
import com.ridefast.ride_fast_backend.dto.WalletTopUpResponse;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository;
import com.ridefast.ride_fast_backend.service.UserService;
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

import java.util.Map;

/**
 * User-facing payment endpoints with /api/v1 prefix
 * For wallet top-up and payment verification
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentController {

    private final UserService userService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApiKeyService apiKeyService;

    @Value("${app.wallet.topup.min-amount:10}")
    private int minTopUpAmount;

    @Value("${app.wallet.topup.max-amount:50000}")
    private int maxTopUpAmount;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Create wallet top-up payment link
     * POST /api/v1/payments/wallet/topup
     */
    @PostMapping("/wallet/topup")
    @PreAuthorize("hasRole('NORMAL_USER')")
    public ResponseEntity<?> createWalletTopUp(
            @Valid @RequestBody WalletTopUpRequest request,
            @RequestHeader("Authorization") String jwtToken) {
        
        try {
            if (request.getAmount().intValue() < minTopUpAmount || request.getAmount().intValue() > maxTopUpAmount) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Amount must be between " + minTopUpAmount + " and " + maxTopUpAmount
                ));
            }

            MyUser user = userService.getRequestedUserProfile(jwtToken);

            // Create initial transaction record
            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(user.getId())
                    .amount(request.getAmount())
                    .currency("INR")
                    .provider("RAZORPAY")
                    .type("WALLET_TOPUP")
                    .status("INITIATED")
                    .notes("Wallet top-up initiated")
                    .build();

            tx = paymentTransactionRepository.save(tx);

            RazorpayClient razorpayClient = new RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());

            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", request.getAmount().multiply(new java.math.BigDecimal("100")).intValue());
            paymentLinkRequest.put("currency", "INR");
            paymentLinkRequest.put("description", "Wallet Top-up");

            JSONObject notes = new JSONObject();
            notes.put("type", "wallet_topup");
            notes.put("user_id", user.getId());
            notes.put("owner_type", request.getOwnerType().toString());
            notes.put("transaction_id", tx.getId());
            paymentLinkRequest.put("notes", notes);

            JSONObject customer = new JSONObject();
            customer.put("name", user.getFullName() != null ? user.getFullName() : "User");
            customer.put("contact", user.getPhone() != null ? user.getPhone() : "");
            customer.put("email", user.getEmail() != null ? user.getEmail() : "");
            paymentLinkRequest.put("customer", customer);

            paymentLinkRequest.put("callback_url", frontendUrl + "/wallet/payment/success");
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
            log.error("Error creating wallet top-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment: " + e.getMessage()));
        }
    }

    /**
     * Verify payment status
     * GET /api/v1/payments/wallet/verify/{transactionId}
     */
    @GetMapping("/wallet/verify/{transactionId}")
    @PreAuthorize("hasRole('NORMAL_USER')")
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
     * Get user's payment transactions
     * GET /api/v1/payments/transactions
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('NORMAL_USER')")
    public ResponseEntity<?> getTransactions(
            @RequestHeader("Authorization") String jwtToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            MyUser user = userService.getRequestedUserProfile(jwtToken);
            var transactions = paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(
                    user.getId(),
                    org.springframework.data.domain.PageRequest.of(page, size)
            );
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions"));
        }
    }
}

