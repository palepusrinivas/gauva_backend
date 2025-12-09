package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.enums.WalletTransactionType;
import com.ridefast.ride_fast_backend.model.Wallet;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import com.ridefast.ride_fast_backend.repository.WalletRepository;
import com.ridefast.ride_fast_backend.repository.WalletTransactionRepository;
import com.ridefast.ride_fast_backend.service.RealtimeService;
import com.ridefast.ride_fast_backend.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

  private final WalletRepository walletRepository;
  private final WalletTransactionRepository txRepository;
  private final RealtimeService realtimeService;

  @Override
  @Transactional
  public Wallet getOrCreate(WalletOwnerType ownerType, String ownerId) {
    return walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
        .orElseGet(() -> walletRepository.save(Wallet.builder()
            .ownerType(ownerType)
            .ownerId(ownerId)
            .balance(java.math.BigDecimal.ZERO)
            .currency("INR")
            .build()));
  }

  @Override
  @Transactional
  public WalletTransaction credit(WalletOwnerType ownerType, String ownerId, BigDecimal amount, String referenceType, String referenceId, String notes) {
    Wallet wallet = getOrCreate(ownerType, ownerId);
    wallet.setBalance(wallet.getBalance().add(amount));
    walletRepository.save(wallet);

    WalletTransaction tx = WalletTransaction.builder()
        .wallet(wallet)
        .type(WalletTransactionType.CREDIT)
        .amount(amount)
        .referenceType(referenceType)
        .referenceId(referenceId)
        .notes(notes)
        .build();
    WalletTransaction savedTx = txRepository.save(tx);
    
    // Broadcast wallet update
    try {
      realtimeService.broadcastWalletUpdate(ownerId, ownerType, wallet.getBalance(), 
          referenceType, notes != null ? notes : "Credit: " + amount);
    } catch (Exception e) {
      log.warn("Failed to broadcast wallet update: {}", e.getMessage());
    }
    
    return savedTx;
  }

  @Override
  @Transactional
  public WalletTransaction debit(WalletOwnerType ownerType, String ownerId, BigDecimal amount, String referenceType, String referenceId, String notes) throws IllegalArgumentException {
    Wallet wallet = getOrCreate(ownerType, ownerId);
    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new IllegalArgumentException("Insufficient wallet balance");
    }
    wallet.setBalance(wallet.getBalance().subtract(amount));
    walletRepository.save(wallet);

    WalletTransaction tx = WalletTransaction.builder()
        .wallet(wallet)
        .type(WalletTransactionType.DEBIT)
        .amount(amount)
        .referenceType(referenceType)
        .referenceId(referenceId)
        .notes(notes)
        .build();
    WalletTransaction savedTx = txRepository.save(tx);
    
    // Broadcast wallet update
    try {
      realtimeService.broadcastWalletUpdate(ownerId, ownerType, wallet.getBalance(), 
          referenceType, notes != null ? notes : "Debit: " + amount);
    } catch (Exception e) {
      log.warn("Failed to broadcast wallet update: {}", e.getMessage());
    }
    
    return savedTx;
  }

  @Override
  public BigDecimal getBalance(WalletOwnerType ownerType, String ownerId) {
    return getOrCreate(ownerType, ownerId).getBalance();
  }

  @Override
  public List<WalletTransaction> listTransactions(WalletOwnerType ownerType, String ownerId) {
    Wallet wallet = getOrCreate(ownerType, ownerId);
    return txRepository.findByWalletOrderByCreatedAtDesc(wallet);
  }
}
