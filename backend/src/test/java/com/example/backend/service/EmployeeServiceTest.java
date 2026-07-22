package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Employee;
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final String EMPLOYEE_NO = "E0001";
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 22);
    private static final String NOT_FOUND_MESSAGE = "対象日時点で有効かつ在職中の社員情報が存在しません。employeeNo=E0001";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeQualificationRepository employeeQualificationRepository;

    @Mock
    private MessageService messageService;

    @Test
    void getEffectiveEmployeeReturnsRepositoryEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmployeeNo(EMPLOYEE_NO);
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(EMPLOYEE_NO, REFERENCE_DATE))
                .thenReturn(employee);

        EmployeeService service = new EmployeeService(
                employeeRepository, employeeQualificationRepository, messageService);

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
        EmployeeService service = new EmployeeService(
                employeeRepository, employeeQualificationRepository, messageService);

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

    @Test
    void getEmployeeDetailMapsEmployeeAndQualifications() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmployeeNo(EMPLOYEE_NO);
        employee.setEmployeeName("Taro Yamada");
        employee.setDepartmentId(2L);
        employee.setBirthDate(LocalDate.of(1990, 1, 2));
        employee.setPostalCode("1000001");
        employee.setAddress("Tokyo");
        employee.setPhoneNumber("09012345678");
        employee.setEmailAddress("taro@example.com");
        employee.setHireDate(LocalDate.of(2020, 4, 1));
        employee.setPositionId(3L);
        employee.setSkillGrade(4);

        EmployeeQualification first = qualification(10L, LocalDate.of(2021, 5, 1));
        EmployeeQualification second = qualification(20L, LocalDate.of(2022, 6, 2));
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(eq(EMPLOYEE_NO), any(LocalDate.class)))
                .thenReturn(employee);
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of(first, second));
        EmployeeService service = new EmployeeService(
                employeeRepository, employeeQualificationRepository, messageService);

        EmployeeDetailDTO result = service.getEmployeeDetail(EMPLOYEE_NO);

        assertThat(result.employeeId()).isEqualTo(1L);
        assertThat(result.employeeNo()).isEqualTo(EMPLOYEE_NO);
        assertThat(result.employeeName()).isEqualTo("Taro Yamada");
        assertThat(result.departmentId()).isEqualTo(2L);
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(result.postalCode()).isEqualTo("1000001");
        assertThat(result.address()).isEqualTo("Tokyo");
        assertThat(result.phoneNumber()).isEqualTo("09012345678");
        assertThat(result.emailAddress()).isEqualTo("taro@example.com");
        assertThat(result.hireDate()).isEqualTo(LocalDate.of(2020, 4, 1));
        assertThat(result.retireDate()).isNull();
        assertThat(result.positionId()).isEqualTo(3L);
        assertThat(result.skillGrade()).isEqualTo(4);
        assertThat(result.qualifications())
                .extracting(qualification -> qualification.qualificationId())
                .containsExactly(10L, 20L);
        assertThat(result.qualifications())
                .extracting(qualification -> qualification.acquisitionDate())
                .containsExactly(LocalDate.of(2021, 5, 1), LocalDate.of(2022, 6, 2));
        verify(employeeQualificationRepository).findByEmployeeIdOrderByAcquisitionDate(1L);
    }

    @Test
    void getEmployeeDetailReturnsEmptyQualificationsWhenNoneAreHeld() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmployeeNo(EMPLOYEE_NO);
        when(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(eq(EMPLOYEE_NO), any(LocalDate.class)))
                .thenReturn(employee);
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of());
        EmployeeService service = new EmployeeService(
                employeeRepository, employeeQualificationRepository, messageService);

        EmployeeDetailDTO result = service.getEmployeeDetail(EMPLOYEE_NO);

        assertThat(result.qualifications()).isEmpty();
    }

    @Test
    void searchEmployeesUsesCurrentDateAndEscapesLikeWildcards() {
        EmployeeListDTO employee = new EmployeeListDTO();
        employee.setEmployeeNo("0012");
        when(employeeRepository.searchEffectiveAndEmployed(
                eq("12!%!_\\"), eq("A!_B"), eq(2L), any(LocalDate.class)))
                .thenReturn(List.of(employee));
        EmployeeService service = new EmployeeService(
                employeeRepository, employeeQualificationRepository, messageService);

        List<EmployeeListDTO> result = service.searchEmployees("12%_\\", "A_B", 2L);

        assertThat(result).containsExactly(employee);
        verify(employeeRepository).searchEffectiveAndEmployed(
                eq("12!%!_\\"), eq("A!_B"), eq(2L), any(LocalDate.class));
    }

    private EmployeeQualification qualification(Long qualificationId, LocalDate acquisitionDate) {
        EmployeeQualification qualification = new EmployeeQualification();
        qualification.setQualificationId(qualificationId);
        qualification.setAcquisitionDate(acquisitionDate);
        return qualification;
    }
}
