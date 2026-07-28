package com.example.backend.service;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.SkillGrade;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SkillGradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillGradeServiceTest {

    @Mock
    private SkillGradeRepository skillGradeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private SkillGradeService skillGradeService;

    @Test
    @DisplayName("職能資格履歴更新")
    void updateSkillGradeTest1() {

        SkillGrade latest =
                new SkillGrade();

        latest.setSkillGrade(1);
        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(skillGradeRepository
                .findLatestBySkillGrade(1))
                .thenReturn(latest);

        when(skillGradeRepository
                .updateEndDate(any()))
                .thenReturn(1);

        SkillGrade result =
                skillGradeService.updateSkillGrade(
                        1,
                        20000L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result.getAllowance())
                .isEqualTo(20000L);
    }

    @Test
    @DisplayName("更新開始日が不正")
    void updateSkillGradeTest2() {

        SkillGrade latest =
                new SkillGrade();

        latest.setStartDate(
                LocalDate.of(2026, 5, 1));
        latest.setEndDate(null);

        when(skillGradeRepository
                .findLatestBySkillGrade(1))
                .thenReturn(latest);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("開始日不正");

        assertThatThrownBy(() ->
                skillGradeService.updateSkillGrade(
                        1,
                        20000L,
                        LocalDate.of(2026, 5, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("終了日更新失敗")
    void updateSkillGradeTest3() {

        SkillGrade latest =
                new SkillGrade();

        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(skillGradeRepository
                .findLatestBySkillGrade(1))
                .thenReturn(latest);

        when(skillGradeRepository
                .updateEndDate(any()))
                .thenReturn(0);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("更新失敗");

        assertThatThrownBy(() ->
                skillGradeService.updateSkillGrade(
                        1,
                        20000L,
                        LocalDate.of(2026, 5, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("未使用の未来履歴は削除できる")
    void deleteSkillGradeTest1() {

        SkillGrade skillGrade =
                new SkillGrade();

        skillGrade.setSkillGrade(1);
        skillGrade.setStartDate(
                LocalDate.now().plusDays(1));
        skillGrade.setEndDate(null);

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(skillGradeRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(skillGrade);

        when(skillGradeRepository.deleteSkillGrade(any()))
                .thenReturn(1);

        skillGradeService.deleteSkillGrade(
                1,
                skillGrade.getStartDate());

        verify(skillGradeRepository)
                .deleteSkillGrade(skillGrade);
    }

    @Test
    @DisplayName("利用中の職能資格は削除できない")
    void deleteSkillGradeTest2() {

        when(employeeRepository.selectCount(any()))
                .thenReturn(1L);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("業務データから参照されているため削除できません。");

        assertThatThrownBy(() ->
                skillGradeService.deleteSkillGrade(
                        1,
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("過去履歴は削除できない")
    void deleteSkillGradeTest3() {

        SkillGrade skillGrade =
                new SkillGrade();

        skillGrade.setStartDate(
                LocalDate.now().minusDays(1));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(skillGradeRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(skillGrade);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("過去履歴削除不可");

        assertThatThrownBy(() ->
                skillGradeService.deleteSkillGrade(
                        1,
                        skillGrade.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("次履歴が存在する場合は削除できない")
    void deleteSkillGradeTest4() {

        SkillGrade skillGrade =
                new SkillGrade();

        skillGrade.setStartDate(
                LocalDate.now().plusDays(1));

        skillGrade.setEndDate(
                LocalDate.now().plusDays(10));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(skillGradeRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(skillGrade);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("開始日が最も先のデータ以外は削除できません。");

        assertThatThrownBy(() ->
                skillGradeService.deleteSkillGrade(
                        1,
                        skillGrade.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }
}