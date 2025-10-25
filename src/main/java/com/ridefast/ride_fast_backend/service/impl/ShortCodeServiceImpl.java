package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

@Service
public class ShortCodeServiceImpl implements ShortCodeService {
  private final UserRepository userRepository;
  private final DriverRepository driverRepository;
  private final RideRepository rideRepository;
  private final SecureRandom random = new SecureRandom();

  public ShortCodeServiceImpl(UserRepository userRepository,
                              DriverRepository driverRepository,
                              RideRepository rideRepository) {
    this.userRepository = userRepository;
    this.driverRepository = driverRepository;
    this.rideRepository = rideRepository;
  }

  private String nextCode() {
    int n = random.nextInt(10000); // 0..9999
    return String.format("%04d", n);
  }

  private String generateUnique(java.util.function.Predicate<String> exists) {
    for (int i = 0; i < 200; i++) { // try up to 200 attempts
      String code = nextCode();
      if (!exists.test(code)) return code;
    }
    throw new IllegalStateException("Unable to generate unique 4-digit code");
  }

  @Override
  public String generateUserCode() {
    return generateUnique(userRepository::existsByShortCode);
  }

  @Override
  public String generateDriverCode() {
    return generateUnique(driverRepository::existsByShortCode);
  }

  @Override
  public String generateRideCode() {
    return generateUnique(rideRepository::existsByShortCode);
  }
}
