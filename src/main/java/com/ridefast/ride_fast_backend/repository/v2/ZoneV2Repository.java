package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneV2Repository extends JpaRepository<ZoneV2, String> {
  Optional<ZoneV2> findByNameAndIsActiveTrue(String name);
}
