package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDriversController {

  private final DriverRepository driverRepository;

  @GetMapping
  public ResponseEntity<Page<Driver>> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(driverRepository.findAll(pageable));
  }

  @GetMapping("/{driverId}")
  public ResponseEntity<Driver> get(@PathVariable Long driverId) {
    return driverRepository.findById(driverId).map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<Driver> create(@RequestBody Driver body) {
    body.setRole(UserRole.DRIVER);
    Driver saved = driverRepository.save(body);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @PutMapping("/{driverId}")
  public ResponseEntity<Driver> update(@PathVariable Long driverId, @RequestBody Driver body) {
    return driverRepository.findById(driverId)
        .map(existing -> {
          if (body.getName() != null) existing.setName(body.getName());
          if (body.getEmail() != null) existing.setEmail(body.getEmail());
          if (body.getMobile() != null) existing.setMobile(body.getMobile());
          if (body.getLatitude() != null) existing.setLatitude(body.getLatitude());
          if (body.getLongitude() != null) existing.setLongitude(body.getLongitude());
          if (body.getRating() != null) existing.setRating(body.getRating());
          return new ResponseEntity<>(driverRepository.save(existing), HttpStatus.OK);
        })
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{driverId}")
  public ResponseEntity<Void> delete(@PathVariable Long driverId) {
    if (!driverRepository.existsById(driverId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    driverRepository.deleteById(driverId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
