package com.ridefast.ride_fast_backend.bootstrap;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BackfillShortCodesRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(BackfillShortCodesRunner.class);

  private final UserRepository userRepository;
  private final DriverRepository driverRepository;
  private final RideRepository rideRepository;
  private final ShortCodeService shortCodeService;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    int users = backfillUsers();
    int drivers = backfillDrivers();
    int rides = backfillRides();
    if (users + drivers + rides > 0) {
      log.info("Backfilled short codes: users={} drivers={} rides={}", users, drivers, rides);
    }
  }

  private int backfillUsers() {
    List<MyUser> list = userRepository.findAll();
    int updated = 0;
    for (MyUser u : list) {
      if (u.getShortCode() == null || u.getShortCode().isBlank()) {
        u.setShortCode(shortCodeService.generateUserCode());
        updated++;
      }
    }
    if (updated > 0) userRepository.saveAll(list);
    return updated;
  }

  private int backfillDrivers() {
    List<Driver> list = driverRepository.findAll();
    int updated = 0;
    for (Driver d : list) {
      if (d.getShortCode() == null || d.getShortCode().isBlank()) {
        d.setShortCode(shortCodeService.generateDriverCode());
        updated++;
      }
    }
    if (updated > 0) driverRepository.saveAll(list);
    return updated;
  }

  private int backfillRides() {
    List<Ride> list = rideRepository.findAll();
    int updated = 0;
    for (Ride r : list) {
      if (r.getShortCode() == null || r.getShortCode().isBlank()) {
        r.setShortCode(shortCodeService.generateRideCode());
        updated++;
      }
    }
    if (updated > 0) rideRepository.saveAll(list);
    return updated;
  }
}
