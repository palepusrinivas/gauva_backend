package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.VehicleModel;
import com.ridefast.ride_fast_backend.model.v2.VehicleBrand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, String> {
  List<VehicleModel> findByBrand(VehicleBrand brand);
}
