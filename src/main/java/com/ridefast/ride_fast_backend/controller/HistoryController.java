package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.UserService;
import com.ridefast.ride_fast_backend.service.WalletService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

  private final UserService userService;
  private final DriverService driverService;
  private final RideRepository rideRepository;
  private final WalletService walletService;

  @GetMapping("/user/rides")
  public ResponseEntity<Page<Ride>> userRides(@RequestHeader("Authorization") String jwtToken,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size)
      throws UserException, ResourceNotFoundException {
    MyUser user = userService.getRequestedUserProfile(jwtToken);
    Pageable pageable = PageRequest.of(page, size);
    Page<Ride> rides = rideRepository.findByUser_Id(user.getId(), pageable);
    return ResponseEntity.ok(rides);
  }

  @GetMapping("/driver/rides")
  public ResponseEntity<Page<Ride>> driverRides(@RequestHeader("Authorization") String jwtToken,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size)
      throws UserException, ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    Pageable pageable = PageRequest.of(page, size);
    Page<Ride> rides = rideRepository.findByDriver_Id(driver.getId(), pageable);
    return ResponseEntity.ok(rides);
  }

  @GetMapping("/user/wallet")
  public ResponseEntity<List<WalletTransaction>> userWallet(@RequestHeader("Authorization") String jwtToken)
      throws UserException, ResourceNotFoundException {
    MyUser user = userService.getRequestedUserProfile(jwtToken);
    return ResponseEntity.ok(walletService.listTransactions(WalletOwnerType.USER, user.getId()));
  }

  @GetMapping("/driver/wallet")
  public ResponseEntity<List<WalletTransaction>> driverWallet(@RequestHeader("Authorization") String jwtToken)
      throws UserException, ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    return ResponseEntity.ok(walletService.listTransactions(WalletOwnerType.DRIVER, driver.getId().toString()));
  }
}
