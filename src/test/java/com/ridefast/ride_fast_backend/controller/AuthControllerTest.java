package com.ridefast.ride_fast_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendResponse;
import com.ridefast.ride_fast_backend.dto.OtpVerifyRequest;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;

    @Test
    void login_returnsOk() throws Exception {
        LoginRequest req = new LoginRequest("user@example.com","pass", UserRole.NORMAL_USER);
        Mockito.when(authService.loginUser(Mockito.any())).thenReturn(JwtResponse.builder().accessToken("t").refreshToken("r").build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void otpLogin_sendOtp_returnsOk() throws Exception {
        OtpSendRequest req = new OtpSendRequest();
        req.setPhoneNumber("+1234567890");
        
        OtpSendResponse response = OtpSendResponse.builder()
                .message("OTP will be sent via Firebase")
                .success(true)
                .phoneNumber("+1234567890")
                .build();
        
        Mockito.when(authService.sendOtp(Mockito.any(OtpSendRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+1234567890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.phoneNumber").value("+1234567890"));
    }

    @Test
    void otpLogin_verifyOtp_returnsOk() throws Exception {
        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .message("Login successfully via OTP")
                .type(UserRole.NORMAL_USER)
                .build();
        
        Mockito.when(authService.verifyOtp(Mockito.any(OtpVerifyRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/api/v1/auth/login/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"firebase-token\",\"role\":\"NORMAL_USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.type").value("NORMAL_USER"));
    }

    @Test
    void otpLogin_invalidRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalidField\":\"value\"}"))
                .andExpect(status().isBadRequest());
    }
}
