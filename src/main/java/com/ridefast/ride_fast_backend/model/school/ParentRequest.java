package com.ridefast.ride_fast_backend.model.school;

import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "parent_requests")
public class ParentRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_user_id")
	private MyUser parentUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id")
	private Branch branch;

	private String studentName;
	private String studentClass;
	private String section;
	private String address;
	private String parentPhone;
	private String parentEmail;
	private String status; // pending/accepted/rejected

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_bus_id")
	private Bus assignedBus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_stop_id")
	private Stop assignedStop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_student_id")
	private Student assignedStudent;

	@CreationTimestamp
	private LocalDateTime createdAt;

	private LocalDateTime processedAt;
}

