package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.Zone;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, String> {
  Optional<Zone> findByReadableIdAndActiveTrue(String readableId);
}
