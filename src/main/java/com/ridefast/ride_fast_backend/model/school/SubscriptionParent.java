package com.ridefast.ride_fast_backend.model.school;

import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "subscriptions_parent")
public class SubscriptionParent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private MyUser user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	private Student student;

	private String planType;
	private Integer price;
	private LocalDate startDate;
	private LocalDate endDate;
	private String status; // active/expired/cancelled
	private Integer scratchBalance = 0;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


