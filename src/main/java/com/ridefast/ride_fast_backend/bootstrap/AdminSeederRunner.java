package com.ridefast.ride_fast_backend.bootstrap;

import com.ridefast.ride_fast_backend.model.AdminUser;
import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeederRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(AdminSeederRunner.class);

  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.admin.username:}")
  private String defaultUsername;

  @Value("${app.admin.password:}")
  private String defaultPassword;

  @Value("${app.admin.role:ROLE_ADMIN}")
  private String defaultRole;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // Seed hardcoded SUPER ADMIN
    final String superUsername = "admin@superadmin";
    final String superPassword = "admin@superadmin123";
    final String superRole = "ROLE_SUPER_ADMIN";
    if (!adminUserRepository.existsByUsername(superUsername)) {
      AdminUser superAdmin = AdminUser.builder()
          .username(superUsername)
          .password(passwordEncoder.encode(superPassword))
          .role(superRole)
          .build();
      adminUserRepository.save(superAdmin);
      log.info("Seeded SuperAdmin username={}", superUsername);
    }

    if (defaultUsername == null || defaultUsername.isBlank() || defaultPassword == null || defaultPassword.isBlank()) {
      return; // no defaults configured; skip
    }
    if (!adminUserRepository.existsByUsername(defaultUsername)) {
      AdminUser admin = AdminUser.builder()
          .username(defaultUsername)
          .password(passwordEncoder.encode(defaultPassword))
          .role(defaultRole)
          .build();
      adminUserRepository.save(admin);
      log.info("Seeded default AdminUser username={}", defaultUsername);
    }
  }
}
