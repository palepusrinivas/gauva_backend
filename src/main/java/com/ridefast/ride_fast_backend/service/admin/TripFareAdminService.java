package com.ridefast.ride_fast_backend.service.admin;

import com.ridefast.ride_fast_backend.dto.admin.TripFareUpsertRequest;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TripFareAdminService {
  Page<TripFare> list(Pageable pageable);
  TripFare upsert(TripFareUpsertRequest req);
  void delete(String tripFareId);
}
