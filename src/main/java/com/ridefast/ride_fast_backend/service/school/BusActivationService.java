package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.BusActivation;
import com.ridefast.ride_fast_backend.repository.school.BusActivationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusActivationService {

	private final BusActivationRepository busActivationRepository;
	private static final int ACTIVATION_FEE = 354;
	private static final int ACTIVATION_DURATION_MONTHS = 6;

	@Transactional
	public BusActivation activateBus(Bus bus) {
		// Deactivate any existing active activation
		Optional<BusActivation> existing = busActivationRepository.findAll().stream()
				.filter(ba -> ba.getBus().getId().equals(bus.getId()) && "active".equals(ba.getStatus()))
				.findFirst();
		
		if (existing.isPresent()) {
			BusActivation old = existing.get();
			old.setStatus("expired");
			busActivationRepository.save(old);
		}

		// Create new activation
		BusActivation activation = new BusActivation();
		activation.setBus(bus);
		activation.setActivationFee(ACTIVATION_FEE);
		activation.setStartDate(LocalDate.now());
		activation.setEndDate(LocalDate.now().plusMonths(ACTIVATION_DURATION_MONTHS));
		activation.setStatus("active");

		return busActivationRepository.save(activation);
	}

	public Optional<BusActivation> findActiveByBus(Bus bus) {
		return busActivationRepository.findAll().stream()
				.filter(ba -> ba.getBus().getId().equals(bus.getId()) && "active".equals(ba.getStatus()))
				.findFirst();
	}

	public boolean isBusActive(Bus bus) {
		return findActiveByBus(bus).isPresent();
	}
}

