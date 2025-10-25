package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
  Optional<AdminUser> findByUsername(String username);
  boolean existsByUsername(String username);
}
