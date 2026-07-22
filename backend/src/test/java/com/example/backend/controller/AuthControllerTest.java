package com.example.backend.controller;

import com.example.backend.dto.auth.LoginResponse;
import com.example.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void loginReturnsResponseFromService() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("token-value", 1L, "E0001", "Taro Yamada"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"E0001","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.employeeNo").value("E0001"))
                .andExpect(jsonPath("$.employeeName").value("Taro Yamada"));
    }

    @Test
    void loginRejectsMissingEmployeeNo() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"password"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsUnauthorizedForUnknownEmployee() throws Exception {
        when(authService.login(any())).thenThrow(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "社員番号またはパスワードが正しくありません。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"UNKNOWN","password":"password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsUnauthorizedForMismatchedPassword() throws Exception {
        when(authService.login(any())).thenThrow(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "社員番号またはパスワードが正しくありません。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"E0001","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
