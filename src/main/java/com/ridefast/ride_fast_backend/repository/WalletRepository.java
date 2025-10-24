package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
  Optional<Wallet> findByOwnerTypeAndOwnerId(WalletOwnerType ownerType, String ownerId);
}
