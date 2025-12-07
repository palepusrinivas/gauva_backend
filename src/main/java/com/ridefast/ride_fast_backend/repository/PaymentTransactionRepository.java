package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
  List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
  Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
  List<PaymentTransaction> findByDriverIdOrderByCreatedAtDesc(Long driverId);
  List<PaymentTransaction> findByRideIdOrderByCreatedAtDesc(Long rideId);

  Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
  
  // For admin panel
  Page<PaymentTransaction> findByStatus(String status, Pageable pageable);
  Page<PaymentTransaction> findByType(String type, Pageable pageable);
  Page<PaymentTransaction> findByStatusAndType(String status, String type, Pageable pageable);
  Page<PaymentTransaction> findByProvider(String provider, Pageable pageable);

  @Query("select sum(t.amount) from PaymentTransaction t where t.status = :status")
  BigDecimal sumByStatus(@Param("status") String status);

  @Query("select sum(t.amount) from PaymentTransaction t where t.status = :status and t.createdAt >= :since")
  BigDecimal sumByStatusSince(@Param("status") String status, @Param("since") LocalDateTime since);
}
