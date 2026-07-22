package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final String EMPLOYEE_NO = "E0001";
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 22);
    private static final String NOT_FOUND_MESSAGE = "対象日時点で有効かつ在職中の社員情報が存在しません。employeeNo=E0001";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MessageService messageService;

    @Test
    void getEffectiveEmployeeReturnsRepositoryEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmployeeNo(EMPLOYEE_NO);
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(EMPLOYEE_NO, REFERENCE_DATE))
                .thenReturn(employee);

        EmployeeService service = new EmployeeService(employeeRepository, messageService);

        Employee result = service.getEffectiveEmployee(EMPLOYEE_NO, REFERENCE_DATE);

        assertThat(result).isSameAs(employee);
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        verify(employeeRepository).findEffectiveAndEmployedByEmployeeNoAt(EMPLOYEE_NO, REFERENCE_DATE);
    }

    @Test
    void getEffectiveEmployeeThrowsNotFoundWhenRepositoryReturnsNull() {
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(EMPLOYEE_NO, REFERENCE_DATE))
                .thenReturn(null);
        when(messageService.getMessage("error.employee.notfound", EMPLOYEE_NO))
                .thenReturn(NOT_FOUND_MESSAGE);
        EmployeeService service = new EmployeeService(employeeRepository, messageService);

        assertThatThrownBy(() -> service.getEffectiveEmployee(EMPLOYEE_NO, REFERENCE_DATE))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(businessException.getMessage()).isEqualTo(NOT_FOUND_MESSAGE);
                });
        verify(employeeRepository).findEffectiveAndEmployedByEmployeeNoAt(EMPLOYEE_NO, REFERENCE_DATE);
        verify(messageService).getMessage("error.employee.notfound", EMPLOYEE_NO);
    }
}
