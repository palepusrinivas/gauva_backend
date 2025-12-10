package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.impl.AuthServiceImpl;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock DriverRepository driverRepository;
    @Mock RefreshTokenService refreshTokenService;
    @Mock JwtTokenHelper jwtTokenHelper;
    @Mock AuthenticationManager authenticationManager;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock DriverService driverService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ModelMapper modelMapper;
    @Mock ShortCodeService shortCodeService;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void loginUser_success() throws ResourceNotFoundException, UserException {
        LoginRequest req = new LoginRequest("user@example.com","pass", UserRole.NORMAL_USER);
        
        // Mock user repository to return a user
        com.ridefast.ride_fast_backend.model.MyUser mockUser = new com.ridefast.ride_fast_backend.model.MyUser();
        mockUser.setEmail("user@example.com");
        mockUser.setRole(UserRole.NORMAL_USER);
        when(userRepository.findByEmailOrPhone("user@example.com")).thenReturn(java.util.Optional.of(mockUser));
        
        // Mock driver repository to return empty (user found, so driver lookup not needed)
        when(driverRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());
        
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        UserDetails details = User.withUsername("user@example.com").password("enc").authorities("ROLE_NORMAL_USER").build();
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(details);
        when(refreshTokenService.createRefreshToken(eq("user@example.com"), any())).thenReturn(new com.ridefast.ride_fast_backend.model.RefreshToken());
        when(jwtTokenHelper.generateToken("user@example.com")).thenReturn("jwt-token");

        JwtResponse res = authService.loginUser(req);
        assertNotNull(res);
        assertEquals("jwt-token", res.getAccessToken());
    }

    @Test
    void loginUser_badCredentials_throws() throws UserException, ResourceNotFoundException {
        LoginRequest req = new LoginRequest("user@example.com","bad", UserRole.NORMAL_USER);
        
        // Mock user repository to return a user
        com.ridefast.ride_fast_backend.model.MyUser mockUser = new com.ridefast.ride_fast_backend.model.MyUser();
        mockUser.setEmail("user@example.com");
        mockUser.setRole(UserRole.NORMAL_USER);
        when(userRepository.findByEmailOrPhone("user@example.com")).thenReturn(java.util.Optional.of(mockUser));
        
        // Mock driver repository to return empty
        when(driverRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());
        
        // Authentication should throw BadCredentialsException
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));
        
        assertThrows(BadCredentialsException.class, () -> authService.loginUser(req));
    }
}
