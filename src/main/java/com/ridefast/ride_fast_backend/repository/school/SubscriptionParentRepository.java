package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.SubscriptionParent;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.school.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionParentRepository extends JpaRepository<SubscriptionParent, Long> {
	List<SubscriptionParent> findByUser(MyUser user);
	List<SubscriptionParent> findByStudent(Student student);
}


