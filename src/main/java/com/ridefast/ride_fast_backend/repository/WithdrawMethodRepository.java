package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.WithdrawMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawMethodRepository extends JpaRepository<WithdrawMethod, Long> {
    List<WithdrawMethod> findByActiveTrue();
    List<WithdrawMethod> findByActiveFalse();
    List<WithdrawMethod> findAllByOrderByNameAsc();
}

