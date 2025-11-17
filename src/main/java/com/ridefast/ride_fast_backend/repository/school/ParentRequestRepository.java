package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.ParentRequest;
import com.ridefast.ride_fast_backend.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentRequestRepository extends JpaRepository<ParentRequest, Long> {
	List<ParentRequest> findByBranch(Branch branch);
	List<ParentRequest> findByParentUser(MyUser parentUser);
	List<ParentRequest> findByStatus(String status);
}

