package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
  List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
  List<PaymentTransaction> findByDriverIdOrderByCreatedAtDesc(Long driverId);
  List<PaymentTransaction> findByRideIdOrderByCreatedAtDesc(Long rideId);
}
