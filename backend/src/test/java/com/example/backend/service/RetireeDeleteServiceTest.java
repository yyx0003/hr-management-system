package com.example.backend.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;

/**
 * RetireeDeleteServiceの単体テスト。
 * 子テーブルの削除順序（salary_result → attendance → employee_qualification → employee）が
 * 崩れていないことを確認する（外部キー制約が無いため、順序はコード自身で担保する必要がある）。
 */
@ExtendWith(MockitoExtension.class)
class RetireeDeleteServiceTest {

    @Mock
    private SalaryResultRepository salaryResultRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EmployeeQualificationRepository employeeQualificationRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private RetireeDeleteService target;

    private static final Long EMPLOYEE_ID = 123L;

    @BeforeEach
    void setUp() {
        target = new RetireeDeleteService(
                salaryResultRepository,
                attendanceRepository,
                employeeQualificationRepository,
                employeeRepository);
    }

    @Test
    void deleteEmployeeData_全ての子テーブルと本体が対象employeeIdで削除される() {
        target.deleteEmployeeData(EMPLOYEE_ID);

        verify(salaryResultRepository).deleteByEmployeeId(EMPLOYEE_ID);
        verify(attendanceRepository).deleteByEmployeeId(EMPLOYEE_ID);
        verify(employeeQualificationRepository).deleteByEmployeeId(EMPLOYEE_ID);
        verify(employeeRepository).deleteByEmployeeId(EMPLOYEE_ID);

        // 想定外の呼び出しが無いことも確認する
        verifyNoMoreInteractions(
                salaryResultRepository, attendanceRepository, employeeQualificationRepository, employeeRepository);
    }

    @Test
    void deleteEmployeeData_削除順序はsalary_result_attendance_employeeQualification_employeeの順() {
        target.deleteEmployeeData(EMPLOYEE_ID);

        // 4つのモックをまたいだ呼び出し順序を検証する
        InOrder order = inOrder(
                salaryResultRepository, attendanceRepository, employeeQualificationRepository, employeeRepository);

        order.verify(salaryResultRepository).deleteByEmployeeId(EMPLOYEE_ID);
        order.verify(attendanceRepository).deleteByEmployeeId(EMPLOYEE_ID);
        order.verify(employeeQualificationRepository).deleteByEmployeeId(EMPLOYEE_ID);
        order.verify(employeeRepository).deleteByEmployeeId(EMPLOYEE_ID);
    }
}
