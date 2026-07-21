package com.example.backend.service;

import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.LoginResponse;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.security.JwtProperties;
import com.example.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MessageSource messageSource;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(new JwtProperties(TEST_SECRET, 60));

    @Test
    void loginReturnsJwtForMatchingCredentials() {
        Employee employee = employee(1L, "E0001", "Taro Yamada", passwordEncoder.encode("password"));
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(eq("E0001"), any(LocalDate.class)))
                .thenReturn(employee);

        AuthService service = new AuthService(employeeRepository, passwordEncoder, jwtTokenProvider, messageSource);
        LoginResponse response = service.login(new LoginRequest("E0001", "password"));

        Claims claims = jwtTokenProvider.parseClaims(response.token());
        assertThat(response.employeeId()).isEqualTo(1L);
        assertThat(response.employeeNo()).isEqualTo("E0001");
        assertThat(response.employeeName()).isEqualTo("Taro Yamada");
        assertThat(claims.getSubject()).isEqualTo("E0001");
        assertThat(claims.get("employeeId", Long.class)).isEqualTo(1L);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void loginRejectsUnknownEmployee() {
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(eq("E0001"), any(LocalDate.class)))
                .thenReturn(null);
        when(messageSource.getMessage(eq("scr010.auth.failed"), eq(null), any())).thenReturn("authentication failed");

        AuthService service = new AuthService(employeeRepository, passwordEncoder, jwtTokenProvider, messageSource);

        assertThatThrownBy(() -> service.login(new LoginRequest("E0001", "password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginRejectsMismatchedPassword() {
        Employee employee = employee(1L, "E0001", "Taro Yamada", passwordEncoder.encode("correct-password"));
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(eq("E0001"), any(LocalDate.class)))
                .thenReturn(employee);
        when(messageSource.getMessage(eq("scr010.auth.failed"), eq(null), any())).thenReturn("authentication failed");

        AuthService service = new AuthService(employeeRepository, passwordEncoder, jwtTokenProvider, messageSource);

        assertThatThrownBy(() -> service.login(new LoginRequest("E0001", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Employee employee(Long employeeId, String employeeNo, String employeeName, String passwordHash) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeNo(employeeNo);
        employee.setEmployeeName(employeeName);
        employee.setPasswordHash(passwordHash);
        return employee;
    }
}
