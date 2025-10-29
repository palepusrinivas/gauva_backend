package com.ridefast.ride_fast_backend;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
@Log4j2
public class RideFastBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RideFastBackendApplication.class, args);
	}

}
