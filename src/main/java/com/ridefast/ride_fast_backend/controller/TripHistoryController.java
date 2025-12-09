package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.RideDto;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ridefast.ride_fast_backend.exception.UserException;

@RestController
@RequestMapping("/api/v1/ride")
@RequiredArgsConstructor
public class TripHistoryController {

  private final RideRepository rideRepository;
  private final UserService userService;
  private final DriverService driverService;
  private final ModelMapper modelMapper;

  @GetMapping("/user/history")
  public ResponseEntity<Page<RideDto>> userHistory(
      @RequestHeader("Authorization") String jwtToken,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "size", required = false, defaultValue = "10") int size) throws UserException, ResourceNotFoundException {
    MyUser user = userService.getRequestedUserProfile(jwtToken);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime", "id"));
    Page<Ride> rides = rideRepository.findByUser_Id(user.getId(), pageable);
    Page<RideDto> dtoPage = rides.map(r -> modelMapper.map(r, RideDto.class));
    return ResponseEntity.ok(dtoPage);
  }

  @GetMapping("/driver/history")
  public ResponseEntity<Page<RideDto>> driverHistory(
      @RequestHeader("Authorization") String jwtToken,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "size", required = false, defaultValue = "10") int size) throws UserException, ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime", "id"));
    Page<Ride> rides = rideRepository.findByDriver_Id(driver.getId(), pageable);
    Page<RideDto> dtoPage = rides.map(r -> modelMapper.map(r, RideDto.class));
    return ResponseEntity.ok(dtoPage);
  }
}
