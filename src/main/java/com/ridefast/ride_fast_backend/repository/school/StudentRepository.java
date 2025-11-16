package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
	List<Student> findByBranch(Branch branch);
}


