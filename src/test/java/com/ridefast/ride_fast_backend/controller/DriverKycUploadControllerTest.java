package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.service.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverKycUploadController.class)
class DriverKycUploadControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean StorageService storageService;

    @Test
    void upload_photo_aadhaar_license_to_firebase_storage() throws Exception {
        // Mock storage upload to return a gs:// path
        Mockito.when(storageService.uploadDriverDocument(any(byte[].class), any(String.class), any(String.class)))
                .thenReturn("gs://test-bucket/path");

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "photo-bytes".getBytes());
        MockMultipartFile aadhaarFront = new MockMultipartFile(
                "aadhaarFront", "aadhaar_front.jpg", MediaType.IMAGE_JPEG_VALUE, "aadhaar-front".getBytes());
        MockMultipartFile licenseFront = new MockMultipartFile(
                "licenseFront", "license_front.jpg", MediaType.IMAGE_JPEG_VALUE, "license-front".getBytes());

        mockMvc.perform(multipart("/api/drivers/{driverId}/kyc/files", 123L)
                        .file(photo)
                        .file(aadhaarFront)
                        .file(licenseFront)
                        .with(user("tester").roles("ADMIN"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoKey").value("drivers/123/kyc/photo.jpg"))
                .andExpect(jsonPath("$.photoGsPath").value("gs://test-bucket/path"))
                .andExpect(jsonPath("$.aadhaarFrontKey").value("drivers/123/kyc/aadhaar_front.jpg"))
                .andExpect(jsonPath("$.aadhaarFrontGsPath").value("gs://test-bucket/path"))
                .andExpect(jsonPath("$.licenseFrontKey").value("drivers/123/kyc/license_front.jpg"))
                .andExpect(jsonPath("$.licenseFrontGsPath").value("gs://test-bucket/path"));
    }
}
