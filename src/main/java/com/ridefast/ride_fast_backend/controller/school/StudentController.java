package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Student;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.service.school.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentController {

	private final BranchRepository branchRepository;
	private final StudentService studentService;

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

	public record UploadResult(int created, int skipped, List<String> errors) {

    }
}


