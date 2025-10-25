package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.ApiKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
  Optional<ApiKey> findByName(String name);
  boolean existsByName(String name);
}
