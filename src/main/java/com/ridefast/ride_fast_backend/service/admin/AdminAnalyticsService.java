package com.ridefast.ride_fast_backend.service.admin;

import com.ridefast.ride_fast_backend.dto.admin.AdminKpiResponse;
import com.ridefast.ride_fast_backend.dto.admin.AdminRecentTrip;
import com.ridefast.ride_fast_backend.dto.admin.AdminZoneStat;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import java.util.List;

public interface AdminAnalyticsService {
  AdminKpiResponse getKpis(int windowDays);
  List<AdminRecentTrip> getRecentTrips(int limit);
  List<AdminZoneStat> getZoneStats(int windowDays);
  List<PaymentTransaction> getRecentTransactions(int limit);
}
