package com.example.backend.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.dto.employee.QualificationDetailDTO;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
    private final MessageService messageService;

    public java.util.List<EmployeeListDTO> searchEmployees(
            String employeeNo, String employeeName, Long departmentId) {
        return employeeRepository.searchEffectiveAndEmployed(
                escapeLike(employeeNo),
                escapeLike(employeeName),
                departmentId,
                LocalDate.now());
    }

    public EmployeeDetailDTO getEmployeeDetail(String employeeNo) {
        Employee employee = getEffectiveEmployee(employeeNo, LocalDate.now());

        var qualifications = employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(employee.getEmployeeId())
                .stream()
                .map(qualification -> new QualificationDetailDTO(
                        qualification.getQualificationId(),
                        qualification.getAcquisitionDate()))
                .toList();

        return new EmployeeDetailDTO(
                employee.getEmployeeId(),
                employee.getEmployeeNo(),
                employee.getEmployeeName(),
                employee.getDepartmentId(),
                employee.getBirthDate(),
                employee.getPostalCode(),
                employee.getAddress(),
                employee.getPhoneNumber(),
                employee.getEmailAddress(),
                employee.getHireDate(),
                employee.getRetireDate(),
                employee.getPositionId(),
                employee.getSkillGrade(),
                qualifications);
    }

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

    private String escapeLike(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
