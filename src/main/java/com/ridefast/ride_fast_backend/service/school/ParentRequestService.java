package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.*;
import com.ridefast.ride_fast_backend.repository.school.ParentRequestRepository;
import com.ridefast.ride_fast_backend.repository.school.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParentRequestService {

	private final ParentRequestRepository parentRequestRepository;
	private final StudentRepository studentRepository;

	public ParentRequest create(ParentRequest request) {
		request.setStatus("pending");
		return parentRequestRepository.save(request);
	}

	@Transactional
	public ParentRequest autoAccept(ParentRequest request, Bus bus, Stop stop) {
		// Create student record
		Student student = new Student();
		student.setName(request.getStudentName());
		student.setStudentClass(request.getStudentClass());
		student.setSection(request.getSection());
		student.setAddress(request.getAddress());
		student.setParentUser(request.getParentUser());
		student.setBranch(request.getBranch());
		student.setBus(bus);
		student.setStop(stop);
		Student savedStudent = studentRepository.save(student);

		// Update request
		request.setStatus("accepted");
		request.setAssignedBus(bus);
		request.setAssignedStop(stop);
		request.setAssignedStudent(savedStudent);
		request.setProcessedAt(LocalDateTime.now());

		return parentRequestRepository.save(request);
	}

	public Optional<ParentRequest> findById(Long id) {
		return parentRequestRepository.findById(id);
	}

	public List<ParentRequest> findByBranch(Branch branch) {
		if (branch == null) {
			return parentRequestRepository.findAll();
		}
		return parentRequestRepository.findByBranch(branch);
	}

	public List<ParentRequest> findByParentUser(com.ridefast.ride_fast_backend.model.MyUser parentUser) {
		return parentRequestRepository.findByParentUser(parentUser);
	}

	public List<ParentRequest> findByStatus(String status) {
		return parentRequestRepository.findByStatus(status);
	}

	public List<ParentRequest> findAll() {
		return parentRequestRepository.findAll();
	}
}

