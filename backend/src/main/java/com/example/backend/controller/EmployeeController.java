package com.example.backend.controller;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{employeeNo}")
    public ResponseEntity<EmployeeDetailDTO> getEmployeeDetail(
            @PathVariable String employeeNo) {

        if (!employeeNo.matches(EMPLOYEE_NO_PATTERN)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    messageService.getMessage("scr040.employeeNo.format"));
        }

        return ResponseEntity.ok(employeeService.getEmployeeDetail(employeeNo));
    }
}
