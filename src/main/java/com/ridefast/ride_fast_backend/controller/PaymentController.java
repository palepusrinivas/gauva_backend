package com.ridefast.ride_fast_backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.razorpay.QrCode;
import com.ridefast.ride_fast_backend.dto.MessageResponse;
import com.ridefast.ride_fast_backend.dto.PaymentLinkResponse;
import com.ridefast.ride_fast_backend.enums.PaymentStatus;
import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.PaymentDetails;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.service.RideService;
import com.ridefast.ride_fast_backend.service.UserService;
import com.ridefast.ride_fast_backend.service.WalletService;
import com.ridefast.ride_fast_backend.service.notification.PushNotificationService;
import com.ridefast.ride_fast_backend.dto.WalletTopUpRequest;
import com.ridefast.ride_fast_backend.dto.WalletTopUpResponse;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.ApiKeyService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {
  private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
  private final UserService userService;
  private final RideService rideService;
  private final RideRepository rideRepository;
  private final DriverRepository driverRepository;
  private final WalletService walletService;
  private final PushNotificationService pushNotificationService;
  private final PaymentTransactionRepository paymentTransactionRepository;
  private final ApiKeyService apiKeyService;

  @Value("${app.wallet.topup.min-amount:10}")
  private int minTopUpAmount;

  @Value("${app.wallet.topup.max-amount:50000}")
  private int maxTopUpAmount;

  @Value("${app.wallet.commission-rate:0.10}")
  private double commissionRate;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @PostMapping("/payments/wallet/topup")
  public ResponseEntity<WalletTopUpResponse> createWalletTopUp(@Valid @RequestBody WalletTopUpRequest request,
      @RequestHeader("Authorization") String jwtToken) throws Exception {

    if (request.getAmount().intValue() < minTopUpAmount || request.getAmount().intValue() > maxTopUpAmount) {
      throw new IllegalArgumentException("Amount must be between " + minTopUpAmount + " and " + maxTopUpAmount);
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
    customer.put("name", user.getFullName());
    customer.put("contact", user.getPhone());
    customer.put("email", user.getEmail());
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
  }

  @GetMapping("/payments/wallet/verify/{transactionId}")
  public ResponseEntity<PaymentTransaction> verifyPayment(@PathVariable Long transactionId) {
    return paymentTransactionRepository.findById(transactionId)
        .map(tx -> new ResponseEntity<>(tx, HttpStatus.OK))
        .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/payments/{rideId}")
  public ResponseEntity<PaymentLinkResponse> createPaymentLink(@PathVariable("rideId") Long rideId,
      @RequestHeader("Authorization") String jwtToken) throws RazorpayException, ResourceNotFoundException {
    Ride ride = rideService.findRideById(rideId);
    try {
      RazorpayClient razorpayClient = new RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());
      JSONObject paymentLinkRequest = new JSONObject();
      paymentLinkRequest.put("amount", (int) Math.round(ride.getFare()) * 100);
      paymentLinkRequest.put("currency", "INR");

      java.util.Map<String, Object> notes = new java.util.HashMap<>();
      notes.put("ride_id", ride.getId());
      if (ride.getDriver() != null)
        notes.put("driver_id", ride.getDriver().getId());
      if (ride.getUser() != null)
        notes.put("user_id", ride.getUser().getId());
      paymentLinkRequest.put("notes", notes);

      java.util.Map<String, Object> customer = new java.util.HashMap<>();
      customer.put("name", ride.getUser().getFullName());
      customer.put("contact", ride.getUser().getPhone());
      customer.put("email", ride.getUser().getEmail());
      paymentLinkRequest.put("customer", customer);

      java.util.Map<String, Object> notify = new java.util.HashMap<>();
      notify.put("sms", Boolean.TRUE);
      notify.put("email", Boolean.TRUE);
      paymentLinkRequest.put("notify", notify);

      paymentLinkRequest.put("reminder_enable", Boolean.TRUE);

      paymentLinkRequest.put("callback_url", frontendUrl + "/ride/" + ride.getId() + "/payment/success");
      paymentLinkRequest.put("callback_method", "get");
      PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

      String paymentLinkId = payment.get("id");
      String paymentLinkUrl = payment.get("short_url");

      PaymentLinkResponse res = new PaymentLinkResponse(paymentLinkUrl, paymentLinkId);
      log.debug("Payment link created id={} url={}", res.getPaymentLinkId(), res.getPaymentLinkUrl());

      return new ResponseEntity<PaymentLinkResponse>(res, HttpStatus.ACCEPTED);

    } catch (Exception e) {
      log.warn("Error creating payment link: {}", e.getMessage());
      throw new RazorpayException(e.getMessage());
    }
  }

  @GetMapping("/payments")
  public ResponseEntity<MessageResponse> redirect(
      @RequestParam(name = "payment_id", required = false) String paymentId,
      @RequestParam(name = "order_id", required = false) Long rideId) throws RazorpayException, ResourceNotFoundException {
    
    // Return error if parameters missing
    if (paymentId == null || rideId == null) {
      return new ResponseEntity<>(new MessageResponse("Missing required parameters: payment_id and order_id"), HttpStatus.BAD_REQUEST);
    }
    RazorpayClient razorpay = new RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());
    Ride ride = rideService.findRideById(rideId);

    try {

      Payment payment = razorpay.payments.fetch(paymentId);
      log.debug("Payment fetched status={}", (Object) payment.get("status"));

      if (payment.get("status").equals("captured")) {
        log.debug("Payment captured for rideId={}", ride.getId());
        if (ride.getPaymentDetails() == null) {
          ride.setPaymentDetails(new PaymentDetails());
        }
        ride.getPaymentDetails().setPaymentId(paymentId);
        ride.getPaymentDetails().setPaymentStatus(PaymentStatus.COMPLETED);

        rideRepository.save(ride);
      }
      MessageResponse res = new MessageResponse("your order get placed");
      return new ResponseEntity<>(res, HttpStatus.OK);

    } catch (Exception e) {
      log.warn("Payment redirect handling failed: {}", e.getMessage());
      // new RedirectView(frontendUrl + "/payment/failed"); // This was doing nothing
      throw new RazorpayException(e.getMessage());
    }

  }

  @PostMapping("/payments/{rideId}/qr")
  public ResponseEntity<JSONObject> createTripQr(@PathVariable("rideId") Long rideId) throws Exception {
    Ride ride = rideService.findRideById(rideId);
    RazorpayClient client = new RazorpayClient(apiKeyService.getRazorpayKeyId(), apiKeyService.getRazorpayKeySecret());
    JSONObject req = new JSONObject();
    req.put("type", "upi_qr");
    req.put("name", ride.getDriver() != null ? ride.getDriver().getName() : "Ride QR");
    req.put("usage", "single_use");
    req.put("fixed_amount", Boolean.TRUE);
    req.put("payment_amount", (int) Math.round(ride.getFare()) * 100);
    req.put("description", "Trip " + ride.getId());
    java.util.Map<String, Object> qrNotes = new java.util.HashMap<>();
    qrNotes.put("ride_id", ride.getId());
    if (ride.getDriver() != null)
      qrNotes.put("driver_id", ride.getDriver().getId());
    if (ride.getUser() != null)
      qrNotes.put("user_id", ride.getUser().getId());
    req.put("notes", qrNotes);

    QrCode qr = client.qrCode.create(req);
    JSONObject resp = new JSONObject();
    resp.put("qr_id", String.valueOf(qr.get("id")));
    resp.put("image_url", String.valueOf(qr.get("image_url")));
    resp.put("amount", String.valueOf(req.get("payment_amount")));
    resp.put("currency", "INR");
    return new ResponseEntity<>(resp, HttpStatus.CREATED);
  }

  @PostMapping("/webhooks/razorpay")
  public ResponseEntity<Void> handleWebhook(@RequestHeader("X-Razorpay-Signature") String signature,
      @RequestBody String payload) {
    try {
      Utils.verifyWebhookSignature(payload, signature, razorpayWebhookSecret);
      JSONObject event = new JSONObject(payload);

      // String entity = event.optString("entity");
      // String eventType = event.optString("event");
      JSONObject payloadObj = event.optJSONObject("payload");

      Long rideId = null;
      String type = null;
      String userId = null;
      Long transactionId = null;
      String paymentId = null;
      Integer amount = null;

      if (payloadObj != null) {
        JSONObject entity = null;
        if (payloadObj.has("payment_link")) {
          entity = payloadObj.getJSONObject("payment_link").getJSONObject("entity");
          amount = entity.optInt("amount_paid", 0);
        } else if (payloadObj.has("payment")) {
          entity = payloadObj.getJSONObject("payment").getJSONObject("entity");
          amount = entity.optInt("amount", 0);
        } else if (payloadObj.has("qr_code")) {
          entity = payloadObj.getJSONObject("qr_code").getJSONObject("entity");
        }

        if (entity != null) {
          paymentId = entity.optString("id");
          JSONObject notes = entity.optJSONObject("notes");
          if (notes != null) {
            if (notes.has("ride_id"))
              rideId = notes.getLong("ride_id");
            if (notes.has("type"))
              type = notes.getString("type");
            if (notes.has("user_id"))
              userId = notes.getString("user_id");
            if (notes.has("driver_id"))
              userId = notes.getString("driver_id"); // Use same variable for driver
            if (notes.has("owner_type"))
              ownerType = notes.getString("owner_type");
            if (notes.has("transaction_id"))
              transactionId = notes.getLong("transaction_id");
          }
        }
      }

      String ownerType = null;
      // Re-parse owner_type from notes if available
      if (payloadObj != null) {
        JSONObject entity = null;
        if (payloadObj.has("payment_link")) {
          entity = payloadObj.getJSONObject("payment_link").getJSONObject("entity");
        } else if (payloadObj.has("payment")) {
          entity = payloadObj.getJSONObject("payment").getJSONObject("entity");
        }
        if (entity != null) {
          JSONObject notes = entity.optJSONObject("notes");
          if (notes != null && notes.has("owner_type")) {
            ownerType = notes.getString("owner_type");
          }
        }
      }

      if ("wallet_topup".equals(type) && userId != null && amount != null) {
        java.math.BigDecimal topUpAmount = java.math.BigDecimal.valueOf(amount)
            .divide(java.math.BigDecimal.valueOf(100));

        // Determine wallet owner type
        WalletOwnerType walletOwnerType = "DRIVER".equals(ownerType) ? WalletOwnerType.DRIVER : WalletOwnerType.USER;

        // Credit the wallet
        walletService.credit(walletOwnerType, userId, topUpAmount, "TOPUP", paymentId,
            "Wallet Top-up via Razorpay");

        // Update transaction status if we have the ID
        if (transactionId != null) {
          String finalPaymentId = paymentId;
          paymentTransactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setStatus("SUCCESS");
            tx.setProviderPaymentId(finalPaymentId);
            paymentTransactionRepository.save(tx);
          });
        }

        // Send notification
        try {
          if ("DRIVER".equals(ownerType)) {
            // Send notification to driver
            com.ridefast.ride_fast_backend.model.Driver driver = driverRepository.findById(Long.parseLong(userId)).orElse(null);
            if (driver != null && driver.getFcmToken() != null && !driver.getFcmToken().isBlank()) {
              java.util.Map<String, String> data = new java.util.HashMap<>();
              data.put("event", "wallet_topup_success");
              data.put("amount", topUpAmount.toString());
              pushNotificationService.sendToToken(driver.getFcmToken(), "Wallet Top-up Successful",
                  "Your wallet has been credited with ₹" + topUpAmount, data);
            }
          } else {
            // Send notification to user
            MyUser user = userService.getUserById(userId);
            if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
              java.util.Map<String, String> data = new java.util.HashMap<>();
              data.put("event", "wallet_topup_success");
              data.put("amount", topUpAmount.toString());
              pushNotificationService.sendToToken(user.getFcmToken(), "Wallet Top-up Successful",
                  "Your wallet has been credited with ₹" + topUpAmount, data);
            }
          }
        } catch (Exception e) {
          log.warn("Failed to send wallet top-up notification: {}", e.getMessage());
        }

      } else if (rideId != null) {
        Ride ride = rideService.findRideById(rideId);
        if (ride.getPaymentDetails() == null)
          ride.setPaymentDetails(new PaymentDetails());
        ride.getPaymentDetails().setPaymentStatus(PaymentStatus.COMPLETED);
        rideRepository.save(ride);

        if (ride.getDriver() != null && ride.getFare() != null) {
          double fare = ride.getFare();
          double commission = fare * commissionRate;
          double net = fare - commission;
          walletService.credit(WalletOwnerType.DRIVER, ride.getDriver().getId().toString(),
              java.math.BigDecimal.valueOf(net), "RIDE", ride.getId().toString(), "Ride fare credit");
        }

        if (ride.getUser() != null && ride.getUser().getFcmToken() != null && !ride.getUser().getFcmToken().isBlank()) {
          java.util.Map<String, String> data = new java.util.HashMap<>();
          data.put("ride_id", String.valueOf(ride.getId()));
          data.put("event", "payment_success");
          pushNotificationService.sendToToken(ride.getUser().getFcmToken(), "Payment received",
              "Your ride payment was successful", data);
        }
      }

      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception ex) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

}
