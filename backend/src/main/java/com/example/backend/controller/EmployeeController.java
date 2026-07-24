package com.example.backend.controller;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.employee.EmployeeCreateRequest;
import com.example.backend.dto.employee.EmployeeCreateResponse;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.dto.employee.EmployeeUpdateRequest;
import com.example.backend.dto.employee.EmployeeUpdateResponse;
import com.example.backend.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final String EMPLOYEE_NO_PATTERN = "[0-9]{1,20}";

    private final EmployeeService employeeService;
    private final MessageService messageService;

    public EmployeeController(EmployeeService employeeService, MessageService messageService) {
        this.employeeService = employeeService;
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<EmployeeCreateResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @PutMapping("/{employeeNo}")
    public ResponseEntity<EmployeeUpdateResponse> updateEmployee(
            @PathVariable String employeeNo,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        validateEmployeeNo(employeeNo);
        return ResponseEntity.ok(employeeService.updateEmployee(employeeNo, request));
    }

    @GetMapping
    public ResponseEntity<java.util.List<EmployeeListDTO>> searchEmployees(
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String departmentId) {

        String normalizedEmployeeNo = normalize(employeeNo);
        String normalizedEmployeeName = normalize(employeeName);
        validateEmployeeNo(normalizedEmployeeNo);
        validateEmployeeName(normalizedEmployeeName);

        return ResponseEntity.ok(employeeService.searchEmployees(
                normalizedEmployeeNo,
                normalizedEmployeeName,
                parseDepartmentId(departmentId)));
    }

    @GetMapping("/{employeeNo}")
    public ResponseEntity<EmployeeDetailDTO> getEmployeeDetail(
            @PathVariable String employeeNo) {

        validateEmployeeNo(employeeNo);

        return ResponseEntity.ok(employeeService.getEmployeeDetail(employeeNo));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateEmployeeNo(String employeeNo) {
        if (employeeNo != null && !employeeNo.matches(EMPLOYEE_NO_PATTERN)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    messageService.getMessage("scr040.employeeNo.format"));
        }
    }

    private void validateEmployeeName(String employeeName) {
        if (employeeName != null && employeeName.length() > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    messageService.getMessage("scr050.employeeName.maxlength"));
        }
    }

    private Long parseDepartmentId(String departmentId) {
        String normalized = normalize(departmentId);
        if (normalized == null || !normalized.matches("[0-9]+")) {
            if (normalized == null) {
                return null;
            }
            throw invalidDepartmentId();
        }

        try {
            long value = Long.parseLong(normalized);
            if (value <= 0) {
                throw invalidDepartmentId();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw invalidDepartmentId();
        }
    }

    private BusinessException invalidDepartmentId() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                messageService.getMessage("scr050.departmentId.invalid"));
    }
}
