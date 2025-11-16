package com.ridefast.ride_fast_backend.controller.school;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class ReportsController {

	@GetMapping("/branches/{branchId}/reports/daily")
	public ResponseEntity<byte[]> dailyReport(@PathVariable Long branchId, @RequestParam String date) {
		String csv = "trip_id,bus_id,driver_id,started_at,ended_at,boarded_count\n";
		// Placeholder CSV
		csv += "1,10,100," + date + "T08:00:00," + date + "T10:00:00,35\n";
		byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily_report_" + branchId + "_" + date + ".csv\"")
				.contentType(MediaType.TEXT_PLAIN)
				.body(bytes);
	}

	@GetMapping("/branches/{branchId}/reports/attendance")
	public ResponseEntity<byte[]> attendanceReport(@PathVariable Long branchId, @RequestParam String date) {
		String csv = "student_id,student_name,stop_id,boarded,trip_time\n";
		csv += "2001,John Doe,501,true," + date + "T08:25:00\n";
		byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance_" + branchId + "_" + date + ".csv\"")
				.contentType(MediaType.TEXT_PLAIN)
				.body(bytes);
	}
}


