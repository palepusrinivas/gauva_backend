package com.ridefast.ride_fast_backend.repository.cashback;

import com.ridefast.ride_fast_backend.model.cashback.CashbackSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashbackSettingsRepository extends JpaRepository<CashbackSettings, Long> {
    
    /**
     * Get the first (and should be only) settings record
     */
    Optional<CashbackSettings> findFirstByOrderByIdAsc();
}

