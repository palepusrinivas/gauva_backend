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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

  // Optional JSON-driven seeding
  // Either provide a path to JSON file or base64 of JSON content
  @Value("${app.admin.seed-json-path:}")
  private String seedJsonPath;

  @Value("${app.admin.seed-json-b64:}")
  private String seedJsonB64;

  // POJO for JSON entries
  public static class AdminSeedEntry {
    public String username;
    public String password;
    public String role;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // 1) If JSON provided, seed from JSON and return
    if (seedJsonAvailable()) {
      try {
        List<AdminSeedEntry> entries = readSeedEntries();
        int created = 0;
        for (AdminSeedEntry e : entries) {
          if (e == null || e.username == null || e.username.isBlank() || e.password == null || e.password.isBlank()) {
            continue;
          }
          String role = (e.role == null || e.role.isBlank()) ? "ROLE_ADMIN" : e.role;
          if (!adminUserRepository.existsByUsername(e.username)) {
            AdminUser admin = AdminUser.builder()
                .username(e.username)
                .password(passwordEncoder.encode(e.password))
                .role(role)
                .build();
            adminUserRepository.save(admin);
            created++;
          }
        }
        log.info("Admin seeding from JSON complete. created={}", created);
        return;
      } catch (Exception ex) {
        log.warn("Admin JSON seeding failed: {}. Falling back to defaults.", ex.getMessage());
      }
    }

    if (defaultUsername == null || defaultUsername.isBlank() || defaultPassword == null || defaultPassword.isBlank()) {
      // 2) Backward-compatible legacy superadmin seeding (kept to avoid breaking environments/tests)
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
        log.info("Seeded legacy SuperAdmin username={}", superUsername);
      }
      return; // no defaults configured; done
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

  private boolean seedJsonAvailable() {
    return (seedJsonB64 != null && !seedJsonB64.isBlank()) || (seedJsonPath != null && !seedJsonPath.isBlank());
  }

  private List<AdminSeedEntry> readSeedEntries() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json;
    if (seedJsonB64 != null && !seedJsonB64.isBlank()) {
      byte[] decoded = Base64.getDecoder().decode(seedJsonB64);
      json = new String(decoded);
    } else {
      Path path = Paths.get(seedJsonPath);
      json = Files.readString(path);
    }
    // Accept either an array of entries or a single object
    json = json.trim();
    if (json.startsWith("[")) {
      return mapper.readValue(json, new TypeReference<List<AdminSeedEntry>>(){});
    } else {
      AdminSeedEntry single = mapper.readValue(json, AdminSeedEntry.class);
      return java.util.List.of(single);
    }
  }
}
