package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Stop;
import com.ridefast.ride_fast_backend.model.school.Student;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.school.StopRepository;
import com.ridefast.ride_fast_backend.repository.school.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

	private final StudentRepository studentRepository;
	private final StopRepository stopRepository;
	private final UserRepository userRepository;

	public record UploadResult(int created, int skipped, List<String> errors) {}

	public UploadResult uploadCsv(Branch branch, MultipartFile file) {
		int created = 0, skipped = 0;
		List<String> errors = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine(); // header
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) continue;
				String[] parts = line.split(",", -1);
				if (parts.length < 8) {
					skipped++;
					errors.add("Invalid row: " + line);
					continue;
				}
				String studentName = parts[0].trim();
				String studentClass = parts[1].trim();
				String section = parts[2].trim();
				String parentPhone = parts[3].trim();
				String parentEmail = parts[4].trim();
				String address = parts[5].trim();
				String assignedStopIdStr = parts[7].trim();

				Stop stop = null;
				if (!assignedStopIdStr.isBlank()) {
					try {
						Long stopId = Long.parseLong(assignedStopIdStr);
						stop = stopRepository.findById(stopId).orElse(null);
					} catch (NumberFormatException ignored) {}
				}

				MyUser parent = null;
				if (!parentPhone.isBlank() || !parentEmail.isBlank()) {
					parent = userRepository.findByEmailOrPhone(!parentEmail.isBlank() ? parentEmail : parentPhone).orElse(null);
					if (parent == null) {
						parent = MyUser.builder()
								.email(parentEmail.isBlank() ? null : parentEmail)
								.phone(parentPhone.isBlank() ? null : parentPhone)
								.role(UserRole.NORMAL_USER)
								.isActive(true)
								.build();
						parent = userRepository.save(parent);
					}
				}

				Student st = new Student();
				st.setName(studentName);
				st.setStudentClass(studentClass);
				st.setSection(section);
				st.setParentUser(parent);
				st.setBranch(branch);
				st.setStop(stop);
				st.setAddress(address);
				studentRepository.save(st);
				created++;
			}
		} catch (Exception e) {
			errors.add("Failed to parse file: " + e.getMessage());
			return new UploadResult(created, skipped, errors);
		}
		return new UploadResult(created, skipped, errors);
	}

	public Optional<Student> findById(Long id) {
		return studentRepository.findById(id);
	}
}


