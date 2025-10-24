package com.ridefast.ride_fast_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

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
  private final UserService userService;
  private final RideService rideService;
  private final RideRepository rideRepository;
  private final WalletService walletService;

  @Value("${app.razorpay.key-id}")
  private String razorpayKeyId;

  @Value("${app.razorpay.key-secret}")
  private String razorpayKeySecret;

  @Value("${app.razorpay.webhook-secret}")
  private String razorpayWebhookSecret;

  @Value("${app.wallet.commission-rate:0.10}")
  private double commissionRate;

  @PostMapping("/payments/{rideId}")
  public ResponseEntity<PaymentLinkResponse> createPaymentLink(@PathVariable("rideId") Long rideId,
      @RequestHeader("Authorization") String jwtToken) throws RazorpayException, ResourceNotFoundException {
    Ride ride = rideService.findRideById(rideId);
    try {
      RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
      JSONObject paymentLinkRequest = new JSONObject();
      paymentLinkRequest.put("amount", (int) Math.round(ride.getFare()) * 100);
      paymentLinkRequest.put("currency", "INR");

      java.util.Map<String, Object> notes = new java.util.HashMap<>();
      notes.put("ride_id", ride.getId());
      if (ride.getDriver() != null) notes.put("driver_id", ride.getDriver().getId());
      if (ride.getUser() != null) notes.put("user_id", ride.getUser().getId());
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

      paymentLinkRequest.put("callback_url", "http://localhost:3000/ride/" + ride.getId() + "/payment/success");
      paymentLinkRequest.put("callback_method", "get");
      PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

      String paymentLinkId = payment.get("id");
      String paymentLinkUrl = payment.get("short_url");

      PaymentLinkResponse res = new PaymentLinkResponse(paymentLinkUrl, paymentLinkId);

      System.out.println("Payment link ID: " + res.getPaymentLinkId());
      System.out.println("Payment link URL: " + res.getPaymentLinkUrl());

      return new ResponseEntity<PaymentLinkResponse>(res, HttpStatus.ACCEPTED);

    } catch (Exception e) {
      System.out.println("Error creating payment link: " + e.getMessage());
      throw new RazorpayException(e.getMessage());
    }
  }

  @GetMapping("/payments")
  public ResponseEntity<MessageResponse> redirect(@RequestParam(name = "payment_id") String paymentId,
      @RequestParam("order_id") Long rideId) throws RazorpayException, ResourceNotFoundException {
    RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    Ride ride = rideService.findRideById(rideId);

    try {

      Payment payment = razorpay.payments.fetch(paymentId);
      System.out.println("payment details --- " + payment + payment.get("status"));

      if (payment.get("status").equals("captured")) {
        System.out.println("payment details --- " + payment + payment.get("status"));
        if (ride.getPaymentDetails() == null) {
          ride.setPaymentDetails(new PaymentDetails());
        }
        ride.getPaymentDetails().setPaymentId(paymentId);
        ride.getPaymentDetails().setPaymentStatus(PaymentStatus.COMPLETED);

        // order.setOrderItems(order.getOrderItems());
        System.out.println(ride.getPaymentDetails().getPaymentStatus() + "payment status ");
        rideRepository.save(ride);
        ;
      }
      MessageResponse res = new MessageResponse("your order get placed");
      return new ResponseEntity<>(res, HttpStatus.OK);

    } catch (Exception e) {
      System.out.println("errrr payment -------- ");
      new RedirectView("http://localhost:3000/payment/failed");
      throw new RazorpayException(e.getMessage());
    }

  }

  @PostMapping("/payments/{rideId}/qr")
  public ResponseEntity<JSONObject> createTripQr(@PathVariable("rideId") Long rideId) throws Exception {
    Ride ride = rideService.findRideById(rideId);
    RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    JSONObject req = new JSONObject();
    req.put("type", "upi_qr");
    req.put("name", ride.getDriver() != null ? ride.getDriver().getName() : "Ride QR");
    req.put("usage", "single_use");
    req.put("fixed_amount", Boolean.TRUE);
    req.put("payment_amount", (int) Math.round(ride.getFare()) * 100);
    req.put("description", "Trip " + ride.getId());
    java.util.Map<String, Object> qrNotes = new java.util.HashMap<>();
    qrNotes.put("ride_id", ride.getId());
    if (ride.getDriver() != null) qrNotes.put("driver_id", ride.getDriver().getId());
    if (ride.getUser() != null) qrNotes.put("user_id", ride.getUser().getId());
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
      Integer amount = null;
      if (payloadObj != null) {
        if (payloadObj.has("payment_link")) {
          JSONObject obj = payloadObj.getJSONObject("payment_link").getJSONObject("entity");
          JSONObject notes = obj.optJSONObject("notes");
          if (notes != null && notes.has("ride_id")) rideId = notes.getLong("ride_id");
          amount = obj.optInt("amount_paid", 0);
        } else if (payloadObj.has("payment")) {
          JSONObject obj = payloadObj.getJSONObject("payment").getJSONObject("entity");
          JSONObject notes = obj.optJSONObject("notes");
          if (notes != null && notes.has("ride_id")) rideId = notes.getLong("ride_id");
          amount = obj.optInt("amount", 0);
        } else if (payloadObj.has("qr_code")) {
          JSONObject obj = payloadObj.getJSONObject("qr_code").getJSONObject("entity");
          JSONObject notes = obj.optJSONObject("notes");
          if (notes != null && notes.has("ride_id")) rideId = notes.getLong("ride_id");
        }
      }

      if (rideId != null) {
        Ride ride = rideService.findRideById(rideId);
        if (ride.getPaymentDetails() == null) ride.setPaymentDetails(new PaymentDetails());
        ride.getPaymentDetails().setPaymentStatus(PaymentStatus.COMPLETED);
        rideRepository.save(ride);

        if (ride.getDriver() != null && ride.getFare() != null) {
          double fare = ride.getFare();
          double commission = fare * commissionRate;
          double net = fare - commission;
          walletService.credit(WalletOwnerType.DRIVER, ride.getDriver().getId().toString(),
              java.math.BigDecimal.valueOf(net), "RIDE", ride.getId().toString(), "Ride fare credit");
        }
      }

      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception ex) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

}
