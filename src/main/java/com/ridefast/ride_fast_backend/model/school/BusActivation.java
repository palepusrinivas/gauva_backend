package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bus_activation")
public class BusActivation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bus_id")
	private Bus bus;

	private Integer activationFee = 354;
	private LocalDate startDate;
	private LocalDate endDate;
	private String status; // active/expired

	@CreationTimestamp
	private LocalDateTime createdAt;
}


