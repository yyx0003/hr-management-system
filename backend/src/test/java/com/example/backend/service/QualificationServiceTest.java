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
import com.example.backend.entity.Qualification;
import com.example.backend.repository.QualificationRepository;

@ExtendWith(MockitoExtension.class)
class QualificationServiceTest {

    @Mock
    private QualificationRepository qualificationRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private QualificationService qualificationService;

    @Test
    @DisplayName("対象日時点で有効な資格を取得できる")
    void findEffectiveAtTest1() {

        Qualification qualification = new Qualification();
        qualification.setQualificationId(1L);
        qualification.setQualificationName("基本情報技術者試験");

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(qualificationRepository.findEffectiveAt(
                1L,
                targetDate))
                .thenReturn(qualification);

        Qualification result =
                qualificationService.findEffectiveAt(
                        1L,
                        targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者試験");
    }

    @Test
    @DisplayName("資格が存在しない場合はBusinessException")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(qualificationRepository.findEffectiveAt(
                1L,
                targetDate))
                .thenReturn(null);

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> qualificationService.findEffectiveAt(
                        1L,
                        targetDate))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("資格一覧を取得できる")
    void findAllEffectiveAtTest1() {

        Qualification qualification = new Qualification();
        qualification.setQualificationId(1L);

        when(qualificationRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(qualification));

        List<Qualification> result =
                qualificationService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("資格一覧が空の場合はBusinessException")
    void findAllEffectiveAtTest2() {

        when(qualificationRepository.findAllEffectiveAt(any()))
                .thenReturn(Collections.emptyList());

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> qualificationService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1)))
                .isInstanceOf(BusinessException.class);
    }
}