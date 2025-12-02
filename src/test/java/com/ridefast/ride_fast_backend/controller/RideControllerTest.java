// package com.ridefast.ride_fast_backend.controller;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.ridefast.ride_fast_backend.dto.RideDto;
// import com.ridefast.ride_fast_backend.dto.RideRequest;
// import com.ridefast.ride_fast_backend.model.MyUser;
// import com.ridefast.ride_fast_backend.model.Ride;
// import com.ridefast.ride_fast_backend.service.DriverService;
// import com.ridefast.ride_fast_backend.service.RideService;
// import com.ridefast.ride_fast_backend.service.UserService;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mockito;
// import org.modelmapper.ModelMapper;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;
//
// import static
// org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
// import static
// org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static
// org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
// @WebMvcTest(RideController.class)
// class RideControllerTest {
//
// @Autowired MockMvc mockMvc;
// @Autowired ObjectMapper objectMapper;
//
// @MockBean RideService rideService;
// @MockBean DriverService driverService;
// @MockBean UserService userService;
// @MockBean ModelMapper modelMapper;
//
// @Test
// void requestRide_unauthenticated_is_unauthorized() throws Exception {
// RideRequest req = RideRequest.builder()
// .pickupArea("A")
// .destinationArea("B")
// .pickupLatitude(10.0)
// .pickupLongitude(20.0)
// .destinationLatitude(11.0)
// .destinationLongitude(21.0)
// .build();
//
// mockMvc.perform(post("/api/v1/ride/request")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(req)))
// .andExpect(status().isUnauthorized());
// }
//
// @Test
// void requestRide_with_driver_role_forbidden() throws Exception {
// RideRequest req = RideRequest.builder()
// .pickupArea("A")
// .destinationArea("B")
// .pickupLatitude(10.0)
// .pickupLongitude(20.0)
// .destinationLatitude(11.0)
// .destinationLongitude(21.0)
// .build();
//
// mockMvc.perform(post("/api/v1/ride/request")
// .with(user("driver@example.com").roles("DRIVER"))
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(req))
// .header("Authorization", "Bearer token"))
// .andExpect(status().isForbidden());
// }
//
// @Test
// void requestRide_with_user_role_returns_ok() throws Exception {
// RideRequest req = RideRequest.builder()
// .pickupArea("A")
// .destinationArea("B")
// .pickupLatitude(10.0)
// .pickupLongitude(20.0)
// .destinationLatitude(11.0)
// .destinationLongitude(21.0)
// .build();
//
// MyUser mockUser = new MyUser();
// mockUser.setId("u1");
// mockUser.setEmail("user@example.com");
//
// Ride ride = Ride.builder().id(1L).build();
// RideDto rideDto = new RideDto();
//
// Mockito.when(userService.getRequestedUserProfile(Mockito.anyString())).thenReturn(mockUser);
// Mockito.when(rideService.requestRide(Mockito.any(RideRequest.class),
// Mockito.any(MyUser.class)))
// .thenReturn(ride);
// Mockito.when(modelMapper.map(Mockito.eq(ride),
// Mockito.eq(RideDto.class))).thenReturn(rideDto);
//
// mockMvc.perform(post("/api/v1/ride/request")
// .with(user("user@example.com").roles("USER"))
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(req))
// .header("Authorization", "Bearer token"))
// .andExpect(status().isOk());
// }
// }
