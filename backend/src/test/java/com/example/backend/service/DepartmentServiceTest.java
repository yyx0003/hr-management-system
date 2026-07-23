package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.MessageSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Department;
import com.example.backend.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("対象日時点で有効な部署を取得できる")
    void findEffectiveAtTest1() {

        LocalDate targetDate =
                LocalDate.of(2026, 7, 1);

        Department department =
                new Department();

        department.setDepartmentId(1L);
        department.setDepartmentName("経営");

        when(departmentRepository.findEffectiveAt(
                1L,
                targetDate))
                .thenReturn(department);

        Department result =
                departmentService.findEffectiveAt(
                        1L,
                        targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentId())
                .isEqualTo(1L);
        assertThat(result.getDepartmentName())
                .isEqualTo("経営");
    }

    @Test
    @DisplayName("対象日時点で有効な部署が存在しない場合はBusinessException")
    void findEffectiveAtTest2() {

        LocalDate targetDate =
                LocalDate.of(2026, 7, 1);

        when(departmentRepository.findEffectiveAt(
                1L,
                targetDate))
                .thenReturn(null);

        when(messageSource.getMessage(
                any(),
                any(),
                any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(() ->
                departmentService.findEffectiveAt(
                        1L,
                        targetDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("対象データが存在しません");
    }

    @Test
    @DisplayName("対象日時点で有効な部署一覧を取得できる")
    void findAllEffectiveAtTest1() {

        LocalDate targetDate =
                LocalDate.of(2026, 7, 1);

        Department department1 =
                new Department();
        department1.setDepartmentId(1L);
        department1.setDepartmentName("経営");

        Department department2 =
                new Department();
        department2.setDepartmentId(2L);
        department2.setDepartmentName("営業");

        List<Department> departments =
                List.of(department1, department2);

        when(departmentRepository.findAllEffectiveAt(
                targetDate))
                .thenReturn(departments);

        List<Department> result =
                departmentService.findAllEffectiveAt(
                        targetDate);

        assertThat(result)
                .hasSize(2);

        assertThat(result)
                .extracting(Department::getDepartmentName)
                .containsExactly("経営", "営業");
    }

    @Test
    @DisplayName("対象日時点で有効な部署一覧が存在しない場合はBusinessException")
    void findAllEffectiveAtTest2() {

        LocalDate targetDate =
                LocalDate.of(2026, 7, 1);

        when(departmentRepository.findAllEffectiveAt(
                targetDate))
                .thenReturn(Collections.emptyList());

        when(messageSource.getMessage(
                any(),
                any(),
                any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(() ->
                departmentService.findAllEffectiveAt(
                        targetDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("対象データが存在しません");
    }
}