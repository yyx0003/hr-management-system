package com.example.backend.service;

import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.LoginResponse;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.security.JwtTokenProvider;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageSource messageSource;

    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       MessageSource messageSource) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageSource = messageSource;
    }

    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(
                request.employeeNo(), LocalDate.now());
        if (employee == null || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw authenticationFailed();
        }

        String token = jwtTokenProvider.createToken(employee.getEmployeeId(), employee.getEmployeeNo());
        return new LoginResponse(token, employee.getEmployeeId(), employee.getEmployeeNo(), employee.getEmployeeName());
    }

    private ResponseStatusException authenticationFailed() {
        String message = messageSource.getMessage("scr010.auth.failed", null, Locale.getDefault());
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
