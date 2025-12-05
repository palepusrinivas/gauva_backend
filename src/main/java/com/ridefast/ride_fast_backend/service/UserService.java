package com.ridefast.ride_fast_backend.service;

import java.util.List;

import com.ridefast.ride_fast_backend.dto.UpdateUserProfileRequest;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.MyUser;

public interface UserService {
  // User createUser(User user);

  MyUser getRequestedUserProfile(String jwtToken) throws ResourceNotFoundException, UserException;

  MyUser getUserById(String userId) throws ResourceNotFoundException;

  List<Ride> getCompletedRides(String userId);

  List<Ride> getUserCurrentRide(String userId) throws ResourceNotFoundException;

  List<Ride> getUserRequestedRide(String userId) throws ResourceNotFoundException;

  MyUser updateProfile(String jwtToken, UpdateUserProfileRequest request) throws ResourceNotFoundException, UserException;
}
