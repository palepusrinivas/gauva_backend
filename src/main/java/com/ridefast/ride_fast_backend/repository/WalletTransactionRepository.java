package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.Wallet;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
  List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}
