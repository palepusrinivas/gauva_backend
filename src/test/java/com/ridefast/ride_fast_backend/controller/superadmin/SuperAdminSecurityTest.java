package com.ridefast.ride_fast_backend.controller.superadmin;

import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import com.ridefast.ride_fast_backend.repository.ApiKeyRepository;
import com.ridefast.ride_fast_backend.service.logs.LogUploadService;
import com.ridefast.ride_fast_backend.service.storage.SignedUrlService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuperAdminController.class)
class SuperAdminSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminUserRepository adminUserRepository;
    @MockBean ApiKeyRepository apiKeyRepository;
    @MockBean PasswordEncoder passwordEncoder;
    @MockBean LogUploadService logUploadService;
    @MockBean SignedUrlService signedUrlService;

    @Test
    void unauthenticated_access_is_unauthorized() throws Exception {
        mockMvc.perform(get("/api/superadmin/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_role_forbidden_superadmin_needed() throws Exception {
        mockMvc.perform(get("/api/superadmin/admins").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superadmin_role_allowed() throws Exception {
        Mockito.when(adminUserRepository.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/superadmin/admins").with(user("sa").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }
}
