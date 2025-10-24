package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Wallet;
import com.ridefast.ride_fast_backend.model.WalletTransaction;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
  Wallet getOrCreate(WalletOwnerType ownerType, String ownerId);

  WalletTransaction credit(WalletOwnerType ownerType, String ownerId, BigDecimal amount, String referenceType, String referenceId, String notes);

  WalletTransaction debit(WalletOwnerType ownerType, String ownerId, BigDecimal amount, String referenceType, String referenceId, String notes) throws IllegalArgumentException;

  BigDecimal getBalance(WalletOwnerType ownerType, String ownerId);

  List<WalletTransaction> listTransactions(WalletOwnerType ownerType, String ownerId);
}
