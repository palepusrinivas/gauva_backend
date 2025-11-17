package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.*;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.school.StudentRepository;
import com.ridefast.ride_fast_backend.repository.school.TrackingPingRepository;
import com.ridefast.ride_fast_backend.repository.school.AlertLogRepository;
import com.ridefast.ride_fast_backend.service.CalculatorService;
import com.ridefast.ride_fast_backend.service.notification.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusAlertService {

	private static final Logger log = LoggerFactory.getLogger(BusAlertService.class);
	private static final double AVERAGE_BUS_SPEED_KMH = 30.0; // Average bus speed in km/h
	private static final int ALERT_THRESHOLD_MINUTES = 5; // Alert when ETA is ~5 minutes

	private final StudentRepository studentRepository;
	private final TrackingPingRepository trackingPingRepository;
	private final AlertLogRepository alertLogRepository;
	private final CalculatorService calculatorService;
	private final PushNotificationService pushNotificationService;

	/**
	 * Calculate ETA in minutes from bus location to stop location
	 */
	public int calculateETA(Double busLat, Double busLng, Double stopLat, Double stopLng) {
		if (busLat == null || busLng == null || stopLat == null || stopLng == null) {
			return -1;
		}

		// Calculate distance in kilometers
		double distanceKm = calculatorService.calculateDistance(busLat, busLng, stopLat, stopLng);

		// Calculate time in minutes: distance (km) / speed (km/h) * 60
		double timeHours = distanceKm / AVERAGE_BUS_SPEED_KMH;
		int timeMinutes = (int) Math.round(timeHours * 60);

		return timeMinutes;
	}

	/**
	 * Check and send alerts for all active students
	 */
	@Transactional
	public void checkAndSendAlerts() {
		log.debug("Starting bus alert check...");

		// Get all students with assigned bus and stop
		List<Student> students = studentRepository.findAll().stream()
			.filter(s -> s.getBus() != null && s.getStop() != null && s.getParentUser() != null)
			.toList();

		log.debug("Found {} students to check", students.size());

		for (Student student : students) {
			try {
				checkStudentAlert(student);
			} catch (Exception e) {
				log.error("Error checking alert for student {}: {}", student.getId(), e.getMessage());
			}
		}
	}

	private void checkStudentAlert(Student student) {
		Bus bus = student.getBus();
		Stop stop = student.getStop();
		MyUser parent = student.getParentUser();

		if (bus == null || stop == null || parent == null) {
			return;
		}

		// Get latest bus location
		var lastPingOpt = trackingPingRepository.findFirstByBusOrderByCreatedAtDesc(bus);
		if (lastPingOpt.isEmpty()) {
			log.debug("No tracking data for bus {}", bus.getId());
			return;
		}

		TrackingPing lastPing = lastPingOpt.get();

		// Check if ping is too old (more than 10 minutes)
		if (lastPing.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
			log.debug("Tracking data too old for bus {}", bus.getId());
			return;
		}

		// Get stop location
		Double stopLat = stop.getLatitude();
		Double stopLng = stop.getLongitude();

		if (stopLat == null || stopLng == null) {
			log.debug("Stop {} has no coordinates", stop.getId());
			return;
		}

		// Calculate ETA
		int etaMinutes = calculateETA(
			lastPing.getLatitude(),
			lastPing.getLongitude(),
			stopLat,
			stopLng
		);

		if (etaMinutes < 0) {
			return;
		}

		// Check if we should send alert (ETA is around threshold)
		if (etaMinutes <= ALERT_THRESHOLD_MINUTES + 1 && etaMinutes >= ALERT_THRESHOLD_MINUTES - 1) {
			// Check if we already sent an alert recently (within last 10 minutes)
			boolean alreadySent = alertLogRepository.findAll().stream()
				.anyMatch(alert -> 
					alert.getUser() != null && alert.getUser().getId().equals(parent.getId()) &&
					alert.getType() != null && alert.getType().equals("bus_eta") &&
					alert.getSentAt() != null &&
					alert.getSentAt().isAfter(LocalDateTime.now().minusMinutes(10))
				);

			if (!alreadySent) {
				sendAlert(student, parent, bus, stop, etaMinutes);
			}
		}
	}

	private void sendAlert(Student student, MyUser parent, Bus bus, Stop stop, int etaMinutes) {
		try {
			String title = "Bus Arrival Alert";
			String body = String.format("Bus %s is approximately %d minutes away from %s stop",
				bus.getBusNumber(), etaMinutes, student.getName());

			Map<String, String> data = new HashMap<>();
			data.put("type", "bus_eta");
			data.put("studentId", String.valueOf(student.getId()));
			data.put("busId", String.valueOf(bus.getId()));
			data.put("stopId", String.valueOf(stop.getId()));
			data.put("etaMinutes", String.valueOf(etaMinutes));

			// Send push notification
			if (parent.getFcmToken() != null && !parent.getFcmToken().isEmpty()) {
				pushNotificationService.sendToToken(parent.getFcmToken(), title, body, data);
				log.info("Sent alert to parent {} for student {} - ETA: {} minutes", 
					parent.getId(), student.getId(), etaMinutes);
			}

			// Log the alert
			AlertLog alertLog = new AlertLog();
			alertLog.setUser(parent);
			alertLog.setType("bus_eta");
			alertLog.setPayload(String.format("studentId=%d,busId=%d,stopId=%d,etaMinutes=%d",
				student.getId(), bus.getId(), stop.getId(), etaMinutes));
			alertLog.setSentAt(LocalDateTime.now());
			alertLogRepository.save(alertLog);

		} catch (Exception e) {
			log.error("Error sending alert: {}", e.getMessage(), e);
		}
	}
}

