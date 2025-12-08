package com.ridefast.ride_fast_backend.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.DriverSignUpRequest;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpLoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendResponse;
import com.ridefast.ride_fast_backend.dto.OtpVerifyRequest;
import com.ridefast.ride_fast_backend.dto.SignUpRequest;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.RefreshToken;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.AuthService;
import com.ridefast.ride_fast_backend.service.CustomUserDetailsService;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.RefreshTokenService;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final DriverRepository driverRepository;
  @Autowired
  private final RefreshTokenService refreshTokenService;
  private final JwtTokenHelper jwtTokenHelper;
  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;

  private final DriverService driverService;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;
  private final ShortCodeService shortCodeService;

  @Override
  public UserResponse signUpUser(SignUpRequest request) throws UserException {

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      boolean emailPresent = userRepository.findByEmail(request.getEmail()).isPresent();
      if (emailPresent) throw new UserException("User already exists with this email");
    }
    boolean phonePresent = userRepository.findByPhone(request.getPhone()).isPresent();
    if (phonePresent) throw new UserException("User already exists with this phone");

    String encodedPassword = passwordEncoder.encode(request.getPassword());

    MyUser createdUser = modelMapper.map(request, MyUser.class);
    createdUser.setPassword(encodedPassword);
    createdUser.setRole(UserRole.NORMAL_USER);
    if (createdUser.getShortCode() == null || createdUser.getShortCode().isBlank()) {
      createdUser.setShortCode(shortCodeService.generateUserCode());
    }
    // Default language to satisfy NOT NULL constraint
    if (createdUser.getCurrentLanguageKey() == null || createdUser.getCurrentLanguageKey().isBlank()) {
      createdUser.setCurrentLanguageKey("en");
    }
    if (createdUser.getFailedAttempt() == null) {
      createdUser.setFailedAttempt(0);
    }
    if (createdUser.getIsActive() == null) {
      createdUser.setIsActive(true);
    }
    if (createdUser.getIsTempBlocked() == null) {
      createdUser.setIsTempBlocked(false);
    }
    if (createdUser.getLoyaltyPoints() == null) {
      createdUser.setLoyaltyPoints(0.0);
    }
    if (createdUser.getCreatedAt() == null) {
      createdUser.setCreatedAt(LocalDateTime.now());
    }

    MyUser savedUser = userRepository.save(createdUser);

    UserResponse userResponse = modelMapper.map(savedUser, UserResponse.class);
    return userResponse;

  }

  @Override
  public JwtResponse loginUser(LoginRequest request) throws ResourceNotFoundException, UserException {
    log.debug("Login attempt for identifier: {}, role: {}", request.getIdentifier(), request.getRole());
    
    // First, check if user exists and validate role before authentication
    MyUser user = userRepository.findByEmailOrPhone(request.getIdentifier()).orElse(null);
    Driver driver = null;
    
    if (user == null) {
      // Try to find as driver
      driver = driverRepository.findByEmail(request.getIdentifier()).orElse(null);
      if (driver == null) {
        log.warn("Login failed: User not found with identifier: {}", request.getIdentifier());
        throw new ResourceNotFoundException("User", "identifier", request.getIdentifier());
      }
      log.debug("Found driver with identifier: {}", request.getIdentifier());
    } else {
      log.debug("Found user with identifier: {}, role: {}", request.getIdentifier(), user.getRole());
    }
    
    // Validate role matches
    if (user != null) {
      // User found - check if role matches
      if (user.getRole() == null || !user.getRole().equals(request.getRole())) {
        log.warn("Login failed: Role mismatch for user {}. Expected: {}, but user has role: {}", 
            request.getIdentifier(), request.getRole(), user.getRole());
        throw new UserException("User role mismatch. Expected: " + request.getRole() + ", but user has role: " + 
            (user.getRole() != null ? user.getRole() : "null"));
      }
    } else if (driver != null) {
      // Driver found - check if role matches
      if (request.getRole() != UserRole.DRIVER) {
        log.warn("Login failed: Role mismatch for driver {}. Expected: {}, but driver role is: DRIVER", 
            request.getIdentifier(), request.getRole());
        throw new UserException("Driver role mismatch. Expected: " + request.getRole() + ", but driver role is: DRIVER");
      }
    }
    
    // Now proceed with authentication
    Authentication authentication = authenticate(request.getIdentifier(), request.getPassword());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getIdentifier());

    RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), request.getRole());

    String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());

    JwtResponse response = JwtResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken.getRefreshToken())
        .type(request.getRole())
        .message("Login successfully : " + userDetails.getUsername())
        .build();
    return response;
  }

  private Authentication authenticate(String username, String password) {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
        password);
    try {
      Authentication authentication = authenticationManager.authenticate(authenticationToken);
      return authentication;
    } catch (BadCredentialsException e) {
      throw new BadCredentialsException("invalid username or password");
    }
  }

  @Override
  public JwtResponse loginUserWithOtp(OtpLoginRequest request) throws ResourceNotFoundException {
    try {
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
      String firebaseUid = decoded.getUid();
      String phone = null;
      Object claimPhone = decoded.getClaims().get("phone_number");
      if (claimPhone instanceof String cp && !cp.isBlank()) {
        phone = cp;
      } else {
        // Fallback to user lookup
        String fetched = FirebaseAuth.getInstance().getUser(firebaseUid).getPhoneNumber();
        if (fetched != null && !fetched.isBlank()) {
          phone = fetched;
        }
      }
      if (phone == null || phone.isBlank()) {
        throw new ResourceNotFoundException("FirebaseToken", "phone", "missing");
      }
      Optional<MyUser> optional = userRepository.findByPhone(phone);
      if (optional.isEmpty()) {
        throw new ResourceNotFoundException("User", "phone", phone);
      }
      MyUser user = optional.get();
      if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
        user.setFirebaseUid(firebaseUid);
      }
      if (user.getPhoneVerifiedAt() == null) {
        user.setPhoneVerifiedAt(LocalDateTime.now());
      }
      userRepository.save(user);

      String principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
      UserDetails userDetails = userDetailsService.loadUserByUsername(principal);
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), request.getRole());
      String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());
      return JwtResponse.builder()
          .accessToken(jwtToken)
          .refreshToken(refreshToken.getRefreshToken())
          .type(request.getRole())
          .message("Login successfully via OTP: " + userDetails.getUsername())
          .build();
    } catch (Exception ex) {
      throw new ResourceNotFoundException("OTP", "verification", ex.getMessage());
    }
  }

  @Override
  public OtpSendResponse sendOtp(OtpSendRequest request) throws ResourceNotFoundException {
    // Note: Firebase Admin SDK cannot directly send OTP codes.
    // OTP sending is handled by Firebase Client SDK on the frontend.
    // This endpoint validates the phone number and returns a response.
    // The client should use Firebase Auth signInWithPhoneNumber() to actually send the OTP.
    
    String phoneNumber = request.getPhoneNumber();
    
    // Normalize phone number format (ensure it starts with +)
    if (!phoneNumber.startsWith("+")) {
      // If phone doesn't start with +, assume it's a local number and add country code
      // For now, we'll just validate the format
      phoneNumber = phoneNumber;
    }
    
    // Validate phone number exists in database (optional - you might want to allow new users)
    Optional<MyUser> userOptional = userRepository.findByPhone(phoneNumber);
    boolean userExists = userOptional.isPresent();
    
    return OtpSendResponse.builder()
        .message("OTP will be sent via Firebase. Please use Firebase Client SDK signInWithPhoneNumber() to send OTP.")
        .success(true)
        .phoneNumber(phoneNumber)
        .build();
  }

  @Override
  public JwtResponse verifyOtp(OtpVerifyRequest request) throws ResourceNotFoundException {
    // This method verifies the Firebase ID token (obtained after client-side OTP verification)
    // and logs in the user
    try {
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
      String firebaseUid = decoded.getUid();
      String phone = null;
      
      // Try to get phone from token claims
      Object claimPhone = decoded.getClaims().get("phone_number");
      if (claimPhone instanceof String cp && !cp.isBlank()) {
        phone = cp;
      } else {
        // Fallback to user lookup from Firebase
        try {
          String fetched = FirebaseAuth.getInstance().getUser(firebaseUid).getPhoneNumber();
          if (fetched != null && !fetched.isBlank()) {
            phone = fetched;
          }
        } catch (Exception e) {
          // If we can't fetch from Firebase, continue with null
        }
      }
      
      if (phone == null || phone.isBlank()) {
        throw new ResourceNotFoundException("FirebaseToken", "phone", "Phone number not found in Firebase token");
      }
      
      // Find user by phone number
      Optional<MyUser> optional = userRepository.findByPhone(phone);
      if (optional.isEmpty()) {
        throw new ResourceNotFoundException("User", "phone", phone);
      }
      
      MyUser user = optional.get();
      
      // Update Firebase UID if not set
      if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
        user.setFirebaseUid(firebaseUid);
      }
      
      // Mark phone as verified
      if (user.getPhoneVerifiedAt() == null) {
        user.setPhoneVerifiedAt(LocalDateTime.now());
      }
      
      userRepository.save(user);

      // Generate JWT tokens
      String principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
      UserDetails userDetails = userDetailsService.loadUserByUsername(principal);
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), request.getRole());
      String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());
      
      return JwtResponse.builder()
          .accessToken(jwtToken)
          .refreshToken(refreshToken.getRefreshToken())
          .type(request.getRole())
          .message("Login successfully via OTP: " + userDetails.getUsername())
          .build();
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception ex) {
      throw new ResourceNotFoundException("OTP", "verification", "Failed to verify OTP: " + ex.getMessage());
    }
  }

  @Override
  public DriverResponse registerDriver(DriverSignUpRequest request) {
    Driver registeredDriver = driverService.registerDriver(request);
    return modelMapper.map(registeredDriver, DriverResponse.class);
  }

}
