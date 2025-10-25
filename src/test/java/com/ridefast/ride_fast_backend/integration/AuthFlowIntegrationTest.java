package com.ridefast.ride_fast_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridefast.ride_fast_backend.bootstrap.AdminSeederRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdminSeederRunner seederRunner;

    record LoginReq(String email,String password,String role){}

    @Test
    void login_then_access_superadmin() throws Exception {
        // ensure seed ran
        seederRunner.run(null);

        var body = objectMapper.writeValueAsString(new LoginReq("admin@superadmin","admin@superadmin123","NORMAL_USER"));
        var loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        var json = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        String token = json.get("accessToken").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/superadmin/admins").header("Authorization","Bearer "+token))
                .andExpect(status().isOk());
    }
}
