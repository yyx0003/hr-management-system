package com.example.backend.service;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Department;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.MessageSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryResultRepository salaryResultRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("対象日時点で有効な部署を取得できる")
    void findEffectiveAtTest1() {

        Department department = new Department();
        department.setDepartmentId(1L);
        department.setDepartmentName("経営");

        when(departmentRepository.findEffectiveAt(
                1L,
                LocalDate.of(2026, 4, 1)))
                        .thenReturn(department);

        Department result =
                departmentService.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result.getDepartmentName())
                .isEqualTo("経営");
    }

    @Test
    @DisplayName("対象部署が存在しない場合")
    void findEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("対象日時点で有効なレコードが存在しません");

        when(departmentRepository.findEffectiveAt(
                any(),
                any()))
                        .thenReturn(null);

        assertThatThrownBy(() ->
                departmentService.findEffectiveAt(
                        999L,
                        LocalDate.now()))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("有効な部署一覧を取得できる")
    void findAllEffectiveAtTest1() {

        Department department = new Department();

        when(departmentRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(department));

        List<Department> result =
                departmentService.findAllEffectiveAt(
                        LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("有効な部署一覧が存在しない")
    void findAllEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("対象日時点で有効なレコードが存在しません");

        when(departmentRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                departmentService.findAllEffectiveAt(
                        LocalDate.now()))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("新規部署登録")
    void createDepartmentTest1() {

        when(departmentRepository.findMaxId())
                .thenReturn(3L);

        Department result =
                departmentService.createDepartment(
                        "テスト部",
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getDepartmentId())
                .isEqualTo(4L);

        verify(departmentRepository)
                .insert(any(Department.class));
    }

    @Test
    @DisplayName("部署テーブルが空の場合はID=1")
    void createDepartmentTest2() {

        when(departmentRepository.findMaxId())
                .thenReturn(null);

        Department result =
                departmentService.createDepartment(
                        "テスト部",
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getDepartmentId())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("部署履歴更新")
    void updateDepartmentTest1() {

        Department latest = new Department();

        latest.setDepartmentId(1L);
        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(departmentRepository
                .findLatestByDepartmentId(1L))
                        .thenReturn(latest);

        when(departmentRepository
                .updateEndDate(any()))
                        .thenReturn(1);

        Department result =
                departmentService.updateDepartment(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新部署");

        assertThat(result.getDepartmentName())
                .isEqualTo("新部署");
    }

    @Test
    @DisplayName("更新開始日が不正")
    void updateDepartmentTest2() {

        Department latest = new Department();

        latest.setStartDate(
                LocalDate.of(2026, 5, 1));
        latest.setEndDate(null);

        when(departmentRepository
                .findLatestByDepartmentId(1L))
                        .thenReturn(latest);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("新しい適用開始日は、現在有効なデータの適用終了日より後の日付にしてください。");

        assertThatThrownBy(() ->
                departmentService.updateDepartment(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新部署"))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("終了日更新失敗")
    void updateDepartmentTest3() {

        Department latest = new Department();

        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(departmentRepository
                .findLatestByDepartmentId(1L))
                        .thenReturn(latest);

        when(departmentRepository
                .updateEndDate(any()))
                        .thenReturn(0);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("更新に失敗しました。");

        assertThatThrownBy(() ->
                departmentService.updateDepartment(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新部署"))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("未使用の未来履歴は削除できる")
    void deleteDepartmentTest1() {

        Department department = new Department();

        department.setDepartmentId(1L);
        department.setStartDate(
                LocalDate.now().plusDays(1));
        department.setEndDate(null);

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(salaryResultRepository.selectCount(any()))
                .thenReturn(0L);

        when(departmentRepository.findEffectiveAt(
                any(),
                any()))
                        .thenReturn(department);

        when(departmentRepository.deleteDepartment(any()))
                .thenReturn(1);
        departmentService.deleteDepartment(
                1L,
                department.getStartDate());

        verify(departmentRepository)
                .deleteDepartment(department);
    }

    @Test
    @DisplayName("利用中の部署は削除できない")
    void deleteDepartmentTest2() {

        when(employeeRepository.selectCount(any()))
                .thenReturn(1L);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("業務データから参照されているため削除できません。");

        assertThatThrownBy(() ->
                departmentService.deleteDepartment(
                        1L,
                        LocalDate.now()))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("過去履歴は削除できない")
    void deleteDepartmentTest3() {

        Department department = new Department();

        department.setStartDate(
                LocalDate.now().minusDays(1));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(salaryResultRepository.selectCount(any()))
                .thenReturn(0L);

        when(departmentRepository.findEffectiveAt(
                any(),
                any()))
                        .thenReturn(department);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("過去の履歴データ、または現在有効なデータは削除できません。");

        assertThatThrownBy(() ->
                departmentService.deleteDepartment(
                        1L,
                        department.getStartDate()))
                                .isInstanceOf(
                                        BusinessException.class);
    }

    @Test
    @DisplayName("次履歴が存在する場合は削除できない")
    void deleteDepartmentTest4() {

        Department department = new Department();

        department.setStartDate(
                LocalDate.now().plusDays(1));

        department.setEndDate(
                LocalDate.now().plusDays(10));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(salaryResultRepository.selectCount(any()))
                .thenReturn(0L);

        when(departmentRepository.findEffectiveAt(
                any(),
                any()))
                        .thenReturn(department);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                        .thenReturn("開始日が最も先のデータ以外は削除できません。");

        assertThatThrownBy(() ->
                departmentService.deleteDepartment(
                        1L,
                        department.getStartDate()))
                                .isInstanceOf(
                                        BusinessException.class);
    }
}