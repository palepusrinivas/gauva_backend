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
@Table(name = "scratch_transactions")
public class ScratchTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_id")
	private SubscriptionParent subscription;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private MyUser user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	private Student student;

	private Integer amount;
	private String type; // win/reverse

	@CreationTimestamp
	private LocalDateTime createdAt;
}


