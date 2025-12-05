package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    
    Page<WithdrawRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<WithdrawRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    List<WithdrawRequest> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    
    Page<WithdrawRequest> findByDriverIdOrderByCreatedAtDesc(Long driverId, Pageable pageable);
    
    @Query("SELECT w FROM WithdrawRequest w WHERE w.driver.name LIKE %:search% OR w.driver.mobile LIKE %:search% ORDER BY w.createdAt DESC")
    Page<WithdrawRequest> searchByDriverNameOrMobile(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT w FROM WithdrawRequest w WHERE w.status = :status AND (w.driver.name LIKE %:search% OR w.driver.mobile LIKE %:search%) ORDER BY w.createdAt DESC")
    Page<WithdrawRequest> searchByStatusAndDriverNameOrMobile(@Param("status") String status, @Param("search") String search, Pageable pageable);
    
    long countByStatus(String status);
    
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawRequest w WHERE w.status = 'SETTLED'")
    java.math.BigDecimal sumSettledAmount();
    
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawRequest w WHERE w.status = 'PENDING'")
    java.math.BigDecimal sumPendingAmount();
}

