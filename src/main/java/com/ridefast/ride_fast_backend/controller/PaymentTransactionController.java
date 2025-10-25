package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;

@RestController
@RequestMapping("/api/payments/history")
@RequiredArgsConstructor
public class PaymentTransactionController {

  private final PaymentTransactionRepository txRepo;
  private final UserService userService;
  private final DriverService driverService;

  @GetMapping("/user")
  public ResponseEntity<List<PaymentTransaction>> userHistory(@RequestHeader("Authorization") String jwtToken) throws UserException, ResourceNotFoundException {
    MyUser user = userService.getRequestedUserProfile(jwtToken);
    return ResponseEntity.ok(txRepo.findByUserIdOrderByCreatedAtDesc(user.getId()));
  }

  @GetMapping("/driver")
  public ResponseEntity<List<PaymentTransaction>> driverHistory(@RequestHeader("Authorization") String jwtToken) throws UserException, ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    return ResponseEntity.ok(txRepo.findByDriverIdOrderByCreatedAtDesc(driver.getId()));
  }

  @GetMapping("/ride/{rideId}")
  public ResponseEntity<List<PaymentTransaction>> rideHistory(@PathVariable Long rideId) {
    return ResponseEntity.ok(txRepo.findByRideIdOrderByCreatedAtDesc(rideId));
  }
}
