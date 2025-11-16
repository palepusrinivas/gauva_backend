package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.SchoolDriver;
import com.ridefast.ride_fast_backend.repository.school.BusRepository;
import com.ridefast.ride_fast_backend.repository.school.SchoolDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusService {

	private final BusRepository busRepository;
	private final SchoolDriverRepository driverRepository;

	public Bus create(Bus bus) {
		return busRepository.save(bus);
	}

	public List<Bus> listByBranch(Branch branch) {
		return busRepository.findByBranch(branch);
	}

	public Optional<Bus> findById(Long id) {
		return busRepository.findById(id);
	}

	public Optional<SchoolDriver> findDriverById(Long id) {
		return driverRepository.findById(id);
	}

	public SchoolDriver assignDriver(Bus bus, SchoolDriver driver) {
		driver.setAssignedBus(bus);
		driver.setIsActive(Boolean.TRUE);
		return driverRepository.save(driver);
	}

	public Bus update(Bus existing, Bus updates) {
		if (updates.getBusNumber() != null) existing.setBusNumber(updates.getBusNumber());
		if (updates.getCapacity() != null) existing.setCapacity(updates.getCapacity());
		if (updates.getType() != null) existing.setType(updates.getType());
		if (updates.getRcExpiry() != null) existing.setRcExpiry(updates.getRcExpiry());
		if (updates.getInsuranceExpiry() != null) existing.setInsuranceExpiry(updates.getInsuranceExpiry());
		if (updates.getPhotoUrl() != null) existing.setPhotoUrl(updates.getPhotoUrl());
		return busRepository.save(existing);
	}

	public void delete(Bus bus) {
		busRepository.delete(bus);
	}
}


