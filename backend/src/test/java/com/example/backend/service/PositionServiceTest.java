package com.example.backend.service;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Position;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.PositionRepository;
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
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PositionService positionService;

    @Test
    @DisplayName("対象日時点で有効な役職を取得できる")
    void findEffectiveAtTest1() {

        Position position = new Position();

        position.setPositionId(1L);
        position.setPositionName("主任");

        when(positionRepository.findEffectiveAt(
                1L,
                LocalDate.of(2026, 4, 1)))
                .thenReturn(position);

        Position result =
                positionService.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result.getPositionName())
                .isEqualTo("主任");
    }

    @Test
    @DisplayName("対象役職が存在しない場合")
    void findEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("対象日時点で有効なレコードが存在しません");

        when(positionRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(null);

        assertThatThrownBy(() ->
                positionService.findEffectiveAt(
                        999L,
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("有効な役職一覧を取得できる")
    void findAllEffectiveAtTest1() {

        Position position = new Position();

        when(positionRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(position));

        List<Position> result =
                positionService.findAllEffectiveAt(
                        LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("有効な役職一覧が存在しない")
    void findAllEffectiveAtTest2() {

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("対象日時点で有効なレコードが存在しません");

        when(positionRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                positionService.findAllEffectiveAt(
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("新規役職登録")
    void createPositionTest1() {

        when(positionRepository.findMaxId())
                .thenReturn(3L);

        Position result =
                positionService.createPosition(
                        "主任",
                        10000L,
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getPositionId())
                .isEqualTo(4L);

        verify(positionRepository)
                .insert(any(Position.class));
    }

    @Test
    @DisplayName("役職テーブルが空の場合はID=1")
    void createPositionTest2() {

        when(positionRepository.findMaxId())
                .thenReturn(null);

        Position result =
                positionService.createPosition(
                        "主任",
                        10000L,
                        LocalDate.of(2027, 1, 1));

        assertThat(result.getPositionId())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("役職履歴更新")
    void updatePositionTest1() {

        Position latest = new Position();

        latest.setPositionId(1L);
        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(positionRepository
                .findLatestByPositionId(1L))
                .thenReturn(latest);

        when(positionRepository
                .updateEndDate(any()))
                .thenReturn(1);

        Position result =
                positionService.updatePosition(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "主任",
                        20000L);

        assertThat(result.getPositionAllowance())
                .isEqualTo(20000L);
    }

    @Test
    @DisplayName("更新開始日が不正")
    void updatePositionTest2() {

        Position latest = new Position();

        latest.setStartDate(
                LocalDate.of(2026, 5, 1));
        latest.setEndDate(null);

        when(positionRepository
                .findLatestByPositionId(1L))
                .thenReturn(latest);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("開始日不正");

        assertThatThrownBy(() ->
                positionService.updatePosition(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "主任",
                        20000L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("終了日更新失敗")
    void updatePositionTest3() {

        Position latest = new Position();

        latest.setStartDate(
                LocalDate.of(2026, 4, 1));
        latest.setEndDate(null);

        when(positionRepository
                .findLatestByPositionId(1L))
                .thenReturn(latest);

        when(positionRepository
                .updateEndDate(any()))
                .thenReturn(0);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("更新失敗");

        assertThatThrownBy(() ->
                positionService.updatePosition(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        "主任",
                        20000L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("未使用の未来履歴は削除できる")
    void deletePositionTest1() {

        Position position = new Position();

        position.setPositionId(1L);
        position.setStartDate(
                LocalDate.now().plusDays(1));
        position.setEndDate(null);

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(positionRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(position);

        when(positionRepository.deletePosition(any()))
                .thenReturn(1);

        positionService.deletePosition(
                1L,
                position.getStartDate());

        verify(positionRepository)
                .deletePosition(position);
    }

    @Test
    @DisplayName("利用中の役職は削除できない")
    void deletePositionTest2() {

        when(employeeRepository.selectCount(any()))
                .thenReturn(1L);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("業務データから参照されているため削除できません。");

        assertThatThrownBy(() ->
                positionService.deletePosition(
                        1L,
                        LocalDate.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("過去履歴は削除できない")
    void deletePositionTest3() {

        Position position = new Position();

        position.setStartDate(
                LocalDate.now().minusDays(1));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(positionRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(position);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("過去履歴削除不可");

        assertThatThrownBy(() ->
                positionService.deletePosition(
                        1L,
                        position.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("次履歴が存在する場合は削除できない")
    void deletePositionTest4() {

        Position position = new Position();

        position.setStartDate(
                LocalDate.now().plusDays(1));

        position.setEndDate(
                LocalDate.now().plusDays(10));

        when(employeeRepository.selectCount(any()))
                .thenReturn(0L);

        when(positionRepository.findEffectiveAt(
                any(),
                any()))
                .thenReturn(position);

        when(messageSource.getMessage(
                any(),
                any(),
                any(Locale.class)))
                .thenReturn("開始日が最も先のデータ以外は削除できません");

        assertThatThrownBy(() ->
                positionService.deletePosition(
                        1L,
                        position.getStartDate()))
                .isInstanceOf(BusinessException.class);
    }
}