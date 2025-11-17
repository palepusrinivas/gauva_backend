package com.ridefast.ride_fast_backend.service.school;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that runs every 2 minutes to check bus locations
 * and send alerts to parents when bus is approaching their stop
 */
@Component
@RequiredArgsConstructor
public class BusAlertScheduler {

	private static final Logger log = LoggerFactory.getLogger(BusAlertScheduler.class);
	private final BusAlertService busAlertService;

	/**
	 * Run every 2 minutes to check for buses approaching stops
	 * Cron: second, minute, hour, day, month, weekday
	 */
	@Scheduled(cron = "0 */2 * * * *")
	public void checkBusAlerts() {
		try {
			log.debug("Running scheduled bus alert check...");
			busAlertService.checkAndSendAlerts();
		} catch (Exception e) {
			log.error("Error in scheduled bus alert check: {}", e.getMessage(), e);
		}
	}
}

