package com.ridefast.ride_fast_backend.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.dto.UpdateUserProfileRequest;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.service.UserService;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository usereRepository;
  private final JwtTokenHelper tokenHelper;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public MyUser getRequestedUserProfile(String jwtToken) throws ResourceNotFoundException, UserException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    MyUser user = null;
    if (usereRepository.findByEmail(email).isPresent())
      user = usereRepository.findByEmail(email).get();
        if (user != null)
      return user;
    throw new UserException("Invalid Jwt Token");
  }

  @Override
  public List<Ride> getCompletedRides(String userId) {
    List<Ride> completedRides = usereRepository.getCompletedRides(userId);

    return completedRides;
  }

  @Override
  public MyUser getUserById(String userId) throws ResourceNotFoundException {
    return usereRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
  }

  @Override
  public List<Ride> getUserCurrentRide(String userId) {
    return usereRepository.getCurrentRides(userId);
  }

  @Override
  public List<Ride> getUserRequestedRide(String userId) throws ResourceNotFoundException {
    return usereRepository.getRequestedRides(userId);
  }

  @Override
  public MyUser updateProfile(String jwtToken, UpdateUserProfileRequest request) throws ResourceNotFoundException, UserException {
    MyUser user = getRequestedUserProfile(jwtToken);
    
    if (request.getFullName() != null && !request.getFullName().isBlank()) {
      user.setFullName(request.getFullName());
    }
    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null && !request.getLastName().isBlank()) {
      user.setLastName(request.getLastName());
    }
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      user.setEmail(request.getEmail());
    }
    if (request.getPhone() != null && !request.getPhone().isBlank()) {
      user.setPhone(request.getPhone());
    }
    if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
      user.setProfileImage(request.getProfileImage());
    }
    if (request.getCurrentLanguageKey() != null && !request.getCurrentLanguageKey().isBlank()) {
      user.setCurrentLanguageKey(request.getCurrentLanguageKey());
    }
    
    return usereRepository.save(user);
  }

  @Override
  public void changePassword(String jwtToken, String currentPassword, String newPassword) throws ResourceNotFoundException, UserException {
    MyUser user = getRequestedUserProfile(jwtToken);
    
    // Verify current password
    if (user.getPassword() == null || user.getPassword().isBlank()) {
      throw new IllegalArgumentException("Cannot change password for social login accounts");
    }
    
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    
    // Validate new password
    if (newPassword.length() < 6) {
      throw new IllegalArgumentException("New password must be at least 6 characters");
    }
    
    // Update password
    user.setPassword(passwordEncoder.encode(newPassword));
    usereRepository.save(user);
  }

}
