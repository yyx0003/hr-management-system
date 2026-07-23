package com.example.backend.dto.employee;

import java.time.LocalDate;
import java.util.List;

public record EmployeeDetailDTO(
        Long employeeId,
        String employeeNo,
        String employeeName,
        Long departmentId,
        LocalDate birthDate,
        String postalCode,
        String address,
        String phoneNumber,
        String emailAddress,
        LocalDate hireDate,
        LocalDate retireDate,
        Long positionId,
        Integer skillGrade,
        List<QualificationDetailDTO> qualifications
) {
}
