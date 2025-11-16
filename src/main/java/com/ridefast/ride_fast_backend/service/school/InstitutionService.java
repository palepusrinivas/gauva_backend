package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.repository.school.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitutionService {

	private final InstitutionRepository institutionRepository;

	public Institution create(Institution toCreate) {
		return institutionRepository.save(toCreate);
	}

	public Optional<Institution> findById(String  uniqueId) {
		return institutionRepository.findByUniqueId(uniqueId);
	}

	public Optional<Institution> findById(Long id) {
		return institutionRepository.findById(id);
	}

	public Institution update(Institution existing, Institution updates) {
		if (updates.getName() != null) existing.setName(updates.getName());
		if (updates.getPrimaryContactName() != null) existing.setPrimaryContactName(updates.getPrimaryContactName());
		if (updates.getPrimaryContactPhone() != null) existing.setPrimaryContactPhone(updates.getPrimaryContactPhone());
		if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
		if (updates.getGstNumber() != null) existing.setGstNumber(updates.getGstNumber());
		return institutionRepository.save(existing);
	}

	public void delete(Institution inst) {
		institutionRepository.delete(inst);
	}
    public String generateUniqueId(){
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        return String.valueOf("INST"+randomInt);
    }

    public List<Institution> findAll() {
        return institutionRepository.findAll();
    }
}


