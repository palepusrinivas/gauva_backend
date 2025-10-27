package com.ridefast.ride_fast_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.AdminUser;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import com.ridefast.ride_fast_backend.enums.UserRole;

import lombok.RequiredArgsConstructor;

// default password will not get generated if we use this service  
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final DriverRepository driverRepository;
  private final UserRepository userRepository;
  private final AdminUserRepository adminUserRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    MyUser user = userRepository.findByEmailOrPhone(username).orElse(null);
    if (user != null) {
      String principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
      return buildUserDetails(principal, user.getPassword(), mapRole(user.getRole()));
    }
    Driver driver = driverRepository.findByEmail(username).orElse(null);
    if (driver != null)
      return buildUserDetails(driver.getEmail(), driver.getPassword(), "ROLE_" + UserRole.DRIVER.name());
    AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);
    if (admin != null)
      return buildUserDetails(admin.getUsername(), admin.getPassword(), admin.getRole());
    throw new UsernameNotFoundException("User or Driver not found");
  }

  private UserDetails buildUserDetails(String username, String password, String role) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (role != null && !role.isBlank()) {
      authorities.add(new SimpleGrantedAuthority(role));
    }
    return new org.springframework.security.core.userdetails.User(username, password, authorities);
  }

  private String mapRole(UserRole role) {
    if (role == null) return null;
    return "ROLE_" + role.name();
  }

}
