package com.ridefast.ride_fast_backend.bootstrap;

import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(AdminSeederRunnerTest.TestConfig.class)
class AdminSeederRunnerTest {

    @Configuration
    static class TestConfig {
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
        @Bean AdminSeederRunner seeder(AdminUserRepository repo, PasswordEncoder enc) { return new AdminSeederRunner(repo, enc); }
    }

    @Autowired AdminSeederRunner seeder;
    @Autowired AdminUserRepository repo;

    @Test
    void seeds_super_admin_when_absent() throws Exception {
        assertFalse(repo.existsByUsername("admin@superadmin"));
        seeder.run(null);
        assertTrue(repo.existsByUsername("admin@superadmin"));
    }
}
