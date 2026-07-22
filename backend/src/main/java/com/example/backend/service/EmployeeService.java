package com.example.backend.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final MessageService messageService;

    public Employee getEffectiveEmployee(String employeeNo, LocalDate referenceDate) {
        Employee employee = employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(
                employeeNo,
                referenceDate);

        if (employee == null) {
            String message = messageService.getMessage("error.employee.notfound", employeeNo);
            throw new BusinessException(HttpStatus.NOT_FOUND, message);
        }

        return employee;
    }
}
