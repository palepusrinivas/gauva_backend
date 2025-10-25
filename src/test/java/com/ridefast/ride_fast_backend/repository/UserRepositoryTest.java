package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.MyUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    @Test
    void findByEmail_and_existsByShortCode_work() {
        MyUser u = MyUser.builder()
                .fullName("Test User")
                .email("u@example.com")
                .password("enc")
                .shortCode("A1B2")
                .role(UserRole.NORMAL_USER)
                .isActive(true)
                .build();
        userRepository.save(u);

        assertThat(userRepository.findByEmail("u@example.com")).isPresent();
        assertThat(userRepository.existsByShortCode("A1B2")).isTrue();
        assertThat(userRepository.existsByShortCode("ZZZZ")).isFalse();
    }
}
