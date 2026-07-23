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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.SkillGrade;
import com.example.backend.repository.SkillGradeRepository;

@ExtendWith(MockitoExtension.class)
class SkillGradeServiceTest {

    @Mock
    private SkillGradeRepository skillGradeRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private SkillGradeService skillGradeService;

    @Test
    @DisplayName("対象日時点で有効な職能資格を取得できる")
    void findEffectiveAtTest1() {

        SkillGrade skillGrade = new SkillGrade();
        skillGrade.setSkillGrade(1);
        skillGrade.setAllowance(200000L);

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(skillGradeRepository.findEffectiveAt(1, targetDate))
                .thenReturn(skillGrade);

        SkillGrade result =
                skillGradeService.findEffectiveAt(1, targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getSkillGrade()).isEqualTo(1);
    }

    @Test
    @DisplayName("職能資格が存在しない場合はBusinessException")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(skillGradeRepository.findEffectiveAt(1, targetDate))
                .thenReturn(null);

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> skillGradeService.findEffectiveAt(1, targetDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("対象データが存在しません");
    }

    @Test
    @DisplayName("職能資格一覧を取得できる")
    void findAllEffectiveAtTest1() {

        SkillGrade skillGrade = new SkillGrade();
        skillGrade.setSkillGrade(1);

        when(skillGradeRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(skillGrade));

        List<SkillGrade> result =
                skillGradeService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("職能資格一覧が空の場合はBusinessException")
    void findAllEffectiveAtTest2() {

        when(skillGradeRepository.findAllEffectiveAt(any()))
                .thenReturn(Collections.emptyList());

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> skillGradeService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1)))
                .isInstanceOf(BusinessException.class);
    }
}