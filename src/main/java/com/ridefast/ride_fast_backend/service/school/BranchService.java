package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BranchService {
@Autowired
	private final BranchRepository branchRepository;

	public Branch create(Branch branch) {
		return branchRepository.save(branch);
	}

	public List<Branch> findByInstitution(Institution institution) {
		return branchRepository.findByInstitution(institution);
	}
    public String BranchuniqueId(){
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        return String.valueOf("BNCH"+randomInt);
    }

    public Branch update(Branch existing, Branch updates) {
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getCity() != null) existing.setCity(updates.getCity());
        if (updates.getState() != null) existing.setState(updates.getState());
        if (updates.getPincode() != null) existing.setPincode(updates.getPincode());
        if (updates.getLatitude() != null) existing.setLatitude(updates.getLatitude());
        if (updates.getLongitude() != null) existing.setLongitude(updates.getLongitude());
        if (updates.getSubscriptionPlan() != null) existing.setSubscriptionPlan(updates.getSubscriptionPlan());
        return branchRepository.save(existing);
    }

    public void delete(Branch branch) {
        branchRepository.delete(branch);
    }
}


