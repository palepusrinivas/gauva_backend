package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Student;
import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.TrackingPing;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.repository.school.TrackingPingRepository;
import com.ridefast.ride_fast_backend.service.school.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentController {

	private final BranchRepository branchRepository;
	private final StudentService studentService;
	private final TrackingPingRepository trackingPingRepository;

	@PostMapping(value = "/branches/{branchId}/students/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UploadResult> uploadStudents(@PathVariable Long branchId, @RequestParam("file") MultipartFile file) {
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null) return ResponseEntity.notFound().build();
		StudentService.UploadResult r = studentService.uploadCsv(branch, file);
		HttpStatus status = r.errors().isEmpty() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
		return new ResponseEntity<>(new UploadResult(r.created(), r.skipped(), r.errors()), status);
	}

	@GetMapping("/students/{id}")
	public ResponseEntity<Student> getStudent(@PathVariable Long id) {
		Optional<Student> st = studentService.findById(id);
		return st.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/students/{id}/tracking")
	public ResponseEntity<?> getStudentTracking(@PathVariable Long id) {
		Optional<Student> studentOpt = studentService.findById(id);
		if (studentOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		
		Student student = studentOpt.get();
		Bus bus = student.getBus();
		
		if (bus == null) {
			return ResponseEntity.ok(Map.of(
				"message", "Student is not assigned to a bus",
				"studentId", id
			));
		}
		
		Optional<TrackingPing> lastPing = trackingPingRepository.findFirstByBusOrderByCreatedAtDesc(bus);
		
		if (lastPing.isEmpty()) {
			return ResponseEntity.ok(Map.of(
				"message", "No tracking data available",
				"studentId", id,
				"busId", bus.getId(),
				"busNumber", bus.getBusNumber()
			));
		}
		
		TrackingPing ping = lastPing.get();
		return ResponseEntity.ok(Map.of(
			"studentId", id,
			"studentName", student.getName(),
			"busId", bus.getId(),
			"busNumber", bus.getBusNumber(),
			"location", Map.of(
				"latitude", ping.getLatitude(),
				"longitude", ping.getLongitude(),
				"speed", ping.getSpeed() != null ? ping.getSpeed() : 0,
				"heading", ping.getHeading() != null ? ping.getHeading() : 0,
				"timestamp", ping.getCreatedAt()
			),
			"stop", student.getStop() != null ? Map.of(
				"id", student.getStop().getId(),
				"name", student.getStop().getName() != null ? student.getStop().getName() : "Unknown"
			) : null
		));
	}

	public record UploadResult(int created, int skipped, List<String> errors) {

    }
}


