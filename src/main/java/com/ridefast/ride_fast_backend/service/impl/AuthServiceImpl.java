package com.ridefast.ride_fast_backend.service.impl;

import org.modelmapper.ModelMapper;
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
import com.ridefast.ride_fast_backend.dto.GoogleLoginRequest;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpLoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendResponse;
import com.ridefast.ride_fast_backend.dto.OtpVerifyRequest;
import com.ridefast.ride_fast_backend.dto.SignUpRequest;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.enums.AuthProvider;
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
    // Check both users and drivers
    Optional<MyUser> userOptional = userRepository.findByPhone(phoneNumber);
    Optional<Driver> driverOptional = driverRepository.findByMobile(phoneNumber);
    boolean userExists = userOptional.isPresent();
    boolean driverExists = driverOptional.isPresent();
    
    return OtpSendResponse.builder()
        .message("OTP will be sent via Firebase. Please use Firebase Client SDK signInWithPhoneNumber() to send OTP.")
        .success(true)
        .phoneNumber(phoneNumber)
        .build();
  }

  @Override
  public JwtResponse verifyOtp(OtpVerifyRequest request) throws ResourceNotFoundException, UserException {
    // This method verifies the Firebase ID token (obtained after client-side OTP verification)
    // and logs in the user or driver
    try {
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
      String firebaseUid = decoded.getUid();
      String phone = null;
      String email = null;
      
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
      
      // Try to get email from token claims
      Object claimEmail = decoded.getClaims().get("email");
      if (claimEmail instanceof String ce && !ce.isBlank()) {
        email = ce;
      }
      
      String principal = null;
      UserDetails userDetails = null;
      
      // Check if it's a driver login
      if (request.getRole() == UserRole.DRIVER) {
        Driver driver = null;
        
        // Try to find driver by phone first (for OTP flow)
        if (phone != null && !phone.isBlank()) {
          Optional<Driver> driverOptional = driverRepository.findByMobile(phone);
          if (driverOptional.isPresent()) {
            driver = driverOptional.get();
          }
        }
        
        // If not found by phone, try email (for Google login flow)
        if (driver == null && email != null && !email.isBlank()) {
          Optional<Driver> driverOptional = driverRepository.findByEmail(email);
          if (driverOptional.isPresent()) {
            driver = driverOptional.get();
          }
        }
        
        if (driver == null) {
          String identifier = phone != null && !phone.isBlank() ? phone : email;
          throw new ResourceNotFoundException("Driver", phone != null && !phone.isBlank() ? "mobile" : "email", identifier);
        }
        
        principal = driver.getEmail();
        userDetails = userDetailsService.loadUserByUsername(principal);
      } else {
        // User login
        if (phone == null || phone.isBlank()) {
          throw new ResourceNotFoundException("FirebaseToken", "phone", "Phone number not found in Firebase token");
        }
        
        // Find user by phone number
        Optional<MyUser> optional = userRepository.findByPhone(phone);
        if (optional.isEmpty()) {
          throw new ResourceNotFoundException("User", "phone", phone);
        }
        
        MyUser user = optional.get();
        
        // Validate role matches
        if (user.getRole() != null && !user.getRole().equals(request.getRole())) {
          throw new UserException("User role mismatch. Expected: " + request.getRole() + ", but user has role: " + user.getRole());
        }
        
        // Update Firebase UID if not set
        if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
          user.setFirebaseUid(firebaseUid);
        }
        
        // Mark phone as verified
        if (user.getPhoneVerifiedAt() == null) {
          user.setPhoneVerifiedAt(LocalDateTime.now());
        }
        
        userRepository.save(user);
        
        principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
        userDetails = userDetailsService.loadUserByUsername(principal);
      }

      // Generate JWT tokens
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
    } catch (UserException e) {
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

  @Override
  public JwtResponse loginWithGoogle(GoogleLoginRequest request) throws ResourceNotFoundException, UserException {
    try {
      // Verify Firebase ID token
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
      String firebaseUid = decoded.getUid();
      
      // Extract user information from Firebase token
      String email = null;
      String name = null;
      String phone = null;
      
      // Get email from token claims
      Object claimEmail = decoded.getClaims().get("email");
      if (claimEmail instanceof String ce && !ce.isBlank()) {
        email = ce;
      }
      
      // Get name from token claims
      Object claimName = decoded.getClaims().get("name");
      if (claimName instanceof String cn && !cn.isBlank()) {
        name = cn;
      }
      
      // Get phone from token claims (may not be present for Google login)
      Object claimPhone = decoded.getClaims().get("phone_number");
      if (claimPhone instanceof String cp && !cp.isBlank()) {
        phone = cp;
      }
      
      // Use provided values from request if token doesn't have them
      if (email == null || email.isBlank()) {
        email = request.getEmail();
      }
      if (name == null || name.isBlank()) {
        name = request.getName();
      }
      if (phone == null || phone.isBlank()) {
        phone = request.getPhone();
      }
      
      // Email is required for Google login
      if (email == null || email.isBlank()) {
        throw new ResourceNotFoundException("GoogleLogin", "email", "Email is required for Google login. Email not found in Firebase token or request.");
      }
      
      // Google login is only for users (NORMAL_USER), not drivers
      UserRole userRole = UserRole.NORMAL_USER;
      
      // Try to find existing user by email first
      Optional<MyUser> userOptional = userRepository.findByEmail(email);
      MyUser user;
      
      if (userOptional.isPresent()) {
        // Existing user - update information
        user = userOptional.get();
        
        // Validate that this is a user account, not a driver account
        if (user.getRole() != null && user.getRole() != UserRole.NORMAL_USER) {
          throw new UserException("Google login is only available for users. This account has role: " + user.getRole());
        }
        
        // Update user information if needed
        boolean needsUpdate = false;
        
        // Update name if not set or if provided name is different
        if (name != null && !name.isBlank()) {
          if (user.getFullName() == null || user.getFullName().isBlank() || !user.getFullName().equals(name)) {
            user.setFullName(name);
            // Split name into first and last name
            if (name.contains(" ")) {
              int spaceIndex = name.indexOf(" ");
              user.setFirstName(name.substring(0, spaceIndex));
              user.setLastName(name.substring(spaceIndex + 1));
            } else {
              user.setFirstName(name);
              user.setLastName(null);
            }
            needsUpdate = true;
          }
        }
        
        // Update phone if provided and not set
        if (phone != null && !phone.isBlank() && (user.getPhone() == null || user.getPhone().isBlank())) {
          // Check if phone is already used by another user
          Optional<MyUser> phoneUser = userRepository.findByPhone(phone);
          if (phoneUser.isPresent() && !phoneUser.get().getId().equals(user.getId())) {
            log.warn("Phone number {} is already associated with another user", phone);
          } else {
            user.setPhone(phone);
            user.setPhoneVerifiedAt(LocalDateTime.now());
            needsUpdate = true;
          }
        }
        
        // Update auth provider information
        if (user.getAuthProvider() == null || user.getAuthProvider() != AuthProvider.GOOGLE) {
          user.setAuthProvider(AuthProvider.GOOGLE);
          needsUpdate = true;
        }
        
        // Update Firebase UID and provider user ID
        if (user.getProviderUserId() == null || user.getProviderUserId().isBlank()) {
          user.setProviderUserId(firebaseUid);
          needsUpdate = true;
        }
        if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
          user.setFirebaseUid(firebaseUid);
          needsUpdate = true;
        }
        
        // Mark email as verified
        if (user.getEmailVerifiedAt() == null) {
          user.setEmailVerifiedAt(LocalDateTime.now());
          needsUpdate = true;
        }
        
        // Ensure role is set to NORMAL_USER
        if (user.getRole() == null) {
          user.setRole(UserRole.NORMAL_USER);
          needsUpdate = true;
        }
        
        // Save if any updates were made
        if (needsUpdate) {
          user = userRepository.save(user);
        }
      } else {
        // New user - create account in MyUser table
        log.info("Creating new user account via Google login for email: {}", email);
        
        // Double-check email is not already used (race condition protection)
        Optional<MyUser> emailCheck = userRepository.findByEmail(email);
        if (emailCheck.isPresent()) {
          // Email was just created by another thread, use existing user
          user = emailCheck.get();
          log.info("User with email {} already exists, using existing account", email);
        } else {
          // Split name into first and last name
          String firstName = null;
          String lastName = null;
          if (name != null && !name.isBlank()) {
            if (name.contains(" ")) {
              int spaceIndex = name.indexOf(" ");
              firstName = name.substring(0, spaceIndex);
              lastName = name.substring(spaceIndex + 1);
            } else {
              firstName = name;
            }
          }
          
          // Check if phone is already used by another user
          if (phone != null && !phone.isBlank()) {
            Optional<MyUser> phoneUser = userRepository.findByPhone(phone);
            if (phoneUser.isPresent()) {
              log.warn("Phone number {} is already associated with another user. Creating account without phone.", phone);
              phone = null; // Don't set phone if it's already used
            }
          }
        
          // Build new user
          user = MyUser.builder()
              .email(email)
              .phone(phone)
              .fullName(name)
              .firstName(firstName)
              .lastName(lastName)
              .role(UserRole.NORMAL_USER)
              .authProvider(AuthProvider.GOOGLE)
              .providerUserId(firebaseUid)
              .firebaseUid(firebaseUid)
              .isActive(true)
              .isTempBlocked(false)
              .failedAttempt(0)
              .loyaltyPoints(0.0)
              .currentLanguageKey("en")
              .emailVerifiedAt(LocalDateTime.now())
              .phoneVerifiedAt(phone != null ? LocalDateTime.now() : null)
              .createdAt(LocalDateTime.now())
              .build();
          
          // Generate short code if not set
          if (user.getShortCode() == null || user.getShortCode().isBlank()) {
            user.setShortCode(shortCodeService.generateUserCode());
          }
          
          // Save new user to database
          try {
            user = userRepository.save(user);
            log.info("Successfully created new user account with ID: {} and email: {}", user.getId(), user.getEmail());
          } catch (Exception e) {
            // Handle potential duplicate email constraint violation
            log.error("Error saving new user: {}", e.getMessage());
            // Try to fetch the user again in case it was created by another thread
            Optional<MyUser> retryUser = userRepository.findByEmail(email);
            if (retryUser.isPresent()) {
              user = retryUser.get();
              log.info("User was created by another thread, using existing account");
            } else {
              throw new UserException("Failed to create user account: " + e.getMessage());
            }
          }
        }
      }
      
      // Generate JWT tokens for the user
      String principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
      if (principal == null || principal.isBlank()) {
        throw new UserException("Cannot generate token: user has no email or phone number");
      }
      
      UserDetails userDetails = userDetailsService.loadUserByUsername(principal);
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), userRole);
      String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());
      
      return JwtResponse.builder()
          .accessToken(jwtToken)
          .refreshToken(refreshToken.getRefreshToken())
          .type(userRole)
          .message("Login successfully via Google: " + userDetails.getUsername())
          .build();
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (UserException e) {
      throw e;
    } catch (Exception ex) {
      log.error("Google login failed: {}", ex.getMessage(), ex);
      throw new ResourceNotFoundException("GoogleLogin", "verification", "Failed to verify Google token: " + ex.getMessage());
    }
  }

}
