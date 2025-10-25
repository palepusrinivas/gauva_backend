package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.VehicleV2;
import com.ridefast.ride_fast_backend.model.v2.VehicleBrand;
import com.ridefast.ride_fast_backend.model.v2.VehicleModel;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleV2Repository extends JpaRepository<VehicleV2, String> {
  List<VehicleV2> findByBrand(VehicleBrand brand);
  List<VehicleV2> findByModel(VehicleModel model);
  List<VehicleV2> findByCategory(VehicleCategory category);
}
