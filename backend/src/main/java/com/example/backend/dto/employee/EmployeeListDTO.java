package com.example.backend.dto.employee;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeListDTO {

    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String departmentName;
    private String positionName;
    private Integer skillGrade;
    private LocalDate hireDate;
}
