package com.example.backend.service;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Qualification;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.QualificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualificationServiceTest {

    @Mock
    private QualificationRepository qualificationRepository;

    @Mock
    private EmployeeQualificationRepository employeeQualificationRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private QualificationService qualificationService;

    @Test
    @DisplayName("対象日時点で有効な資格を取得できる")
    void findEffectiveAtTest1() {

        Qualification qualification = new Qualification();

        qualification.setQualificationId(1L);
        qualification.setQualificationName("基本情報技術者");

        when(qualificationRepository.findEffectiveAt(
                1L,
                LocalDate.of(2026, 4, 1)))
                .thenReturn(qualification);

        Qualification result =
                qualificationService.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者");
    }

    @Test
    @DisplayName("対象資格が存在しない場合")
    void findEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("対象日時点で有効なレコードが存在しません");

        when(qualificationRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(null);

        assertThatThrownBy(() ->
                qualificationService.findEffectiveAt(
                        999L,
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("有効な資格一覧を取得できる")
    void findAllEffectiveAtTest1() {

        Qualification qualification =
                new Qualification();

        when(qualificationRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(qualification));

        List<Qualification> result =
                qualificationService.findAllEffectiveAt(
                        LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("有効な資格一覧が存在しない")
    void findAllEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("対象日時点で有効なレコードが存在しません");

        when(qualificationRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                qualificationService.findAllEffectiveAt(
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("新規資格登録")
    void createQualificationTest1() {

        when(qualificationRepository.findMaxId())
                .thenReturn(3L);

        Qualification result =
                qualificationService.createQualification(
                        "新資格",
                        false,
                        10000L,
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getQualificationId())
                .isEqualTo(4L);

        verify(qualificationRepository)
                .insert(any(Qualification.class));
    }

    @Test
    @DisplayName("資格テーブルが空の場合はID=1")
    void createQualificationTest2() {

        when(qualificationRepository.findMaxId())
                .thenReturn(null);

        Qualification result =
                qualificationService.createQualification(
                        "新資格",
                        false,
                        10000L,
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getQualificationId())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("資格履歴更新")
    void updateQualificationTest1() {

        Qualification latest =
                new Qualification();

        latest.setQualificationId(1L);
        latest.setStartDate(
                LocalDate.of(2026, 4, 1));

        latest.setEndDate(null);

        when(qualificationRepository
                .findLatestByQualificationId(1L))
                .thenReturn(latest);

        when(qualificationRepository
                .updateEndDate(any()))
                .thenReturn(1);

        Qualification result =
                qualificationService.updateQualification(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新資格",
                        false,
                        20000L);

        assertThat(result.getQualificationName())
                .isEqualTo("新資格");
    }

    @Test
    @DisplayName("更新開始日が不正")
    void updateQualificationTest2() {

        Qualification latest =
                new Qualification();

        latest.setStartDate(
                LocalDate.of(2026, 5, 1));

        latest.setEndDate(null);

        when(qualificationRepository
                .findLatestByQualificationId(1L))
                .thenReturn(latest);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("開始日不正");

        assertThatThrownBy(() ->
                qualificationService.updateQualification(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新資格",
                        false,
                        20000L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("終了日更新失敗")
    void updateQualificationTest3() {

        Qualification latest =
                new Qualification();

        latest.setStartDate(
                LocalDate.of(2026, 4, 1));

        latest.setEndDate(null);

        when(qualificationRepository
                .findLatestByQualificationId(1L))
                .thenReturn(latest);

        when(qualificationRepository
                .updateEndDate(any()))
                .thenReturn(0);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("更新失敗");

        assertThatThrownBy(() ->
                qualificationService.updateQualification(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "新資格",
                        false,
                        20000L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("未使用の未来履歴は削除できる")
    void deleteQualificationTest1() {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(1L);
        qualification.setStartDate(
                LocalDate.now().plusDays(1));

        qualification.setEndDate(null);

        when(employeeQualificationRepository.selectCount(any()))
                .thenReturn(0L);

        when(qualificationRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(qualification);

        when(qualificationRepository.deleteQualification(
                qualification)).thenReturn(1);
        
        qualificationService.deleteQualification(
                1L,
                qualification.getStartDate());

        verify(qualificationRepository)
                .deleteQualification(qualification);
    }

    @Test
    @DisplayName("利用中の資格は削除できない")
    void deleteQualificationTest2() {

        when(employeeQualificationRepository.selectCount(any()))
                .thenReturn(1L);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("利用中");

        assertThatThrownBy(() ->
                qualificationService.deleteQualification(
                        1L,
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("過去履歴は削除できない")
    void deleteQualificationTest3() {

        Qualification qualification =
                new Qualification();

        qualification.setStartDate(
                LocalDate.now().minusDays(1));

        when(employeeQualificationRepository.selectCount(any()))
                .thenReturn(0L);

        when(qualificationRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(qualification);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("履歴削除不可");

        assertThatThrownBy(() ->
                qualificationService.deleteQualification(
                        1L,
                        qualification.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("次履歴が存在する場合は削除できない")
    void deleteQualificationTest4() {

        Qualification qualification =
                new Qualification();

        qualification.setStartDate(
                LocalDate.now().plusDays(1));

        qualification.setEndDate(
                LocalDate.now().plusDays(10));

        when(employeeQualificationRepository.selectCount(any()))
                .thenReturn(0L);

        when(qualificationRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(qualification);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("削除不可");

        assertThatThrownBy(() ->
                qualificationService.deleteQualification(
                        1L,
                        qualification.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }
}