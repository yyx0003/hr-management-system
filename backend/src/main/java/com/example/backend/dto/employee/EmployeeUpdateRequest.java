package com.example.backend.dto.employee;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateRequest(
        @NotBlank(message = "{scr030.name.required}")
        @Size(max = 100, message = "{scr030.name.maxlength}")
        String employeeName,
        @NotNull(message = "{scr030.birthDate.required}")
        @PastOrPresent(message = "{scr030.birthDate.future}")
        LocalDate birthDate,
        @NotBlank(message = "{scr030.postalCode.required}")
        @Pattern(regexp = "[0-9]{7}", message = "{scr030.postalCode.format}")
        String postalCode,
        @NotBlank(message = "{scr030.address.required}")
        @Size(max = 255, message = "{scr030.address.maxlength}")
        String address,
        @Pattern(regexp = "[0-9]{1,20}", message = "{scr030.phoneNumber.format}")
        String phoneNumber,
        @Email(message = "{scr030.emailAddress.format}")
        @Size(max = 255, message = "{scr030.emailAddress.maxlength}")
        String emailAddress,
        @NotNull(message = "{scr030.department.required}")
        Long departmentId,
        Long positionId,
        @NotNull(message = "{scr030.skillGrade.required}")
        Integer skillGrade,
        LocalDate retireDate,
        List<@NotNull(message = "{scr040.qualification.required}")
             @Valid EmployeeQualificationUpdateRequest> qualifications) {
}
