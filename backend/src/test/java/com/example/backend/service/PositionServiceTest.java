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
import com.example.backend.entity.Position;
import com.example.backend.repository.PositionRepository;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PositionService positionService;

    @Test
    @DisplayName("対象日時点で有効な役職を取得できる")
    void findEffectiveAtTest1() {

        Position position = new Position();
        position.setPositionId(1L);
        position.setPositionName("部長");

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(positionRepository.findEffectiveAt(1L, targetDate))
                .thenReturn(position);

        Position result =
                positionService.findEffectiveAt(1L, targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionName()).isEqualTo("部長");
    }

    @Test
    @DisplayName("役職が存在しない場合はBusinessException")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        when(positionRepository.findEffectiveAt(1L, targetDate))
                .thenReturn(null);

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> positionService.findEffectiveAt(1L, targetDate))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("役職一覧を取得できる")
    void findAllEffectiveAtTest1() {

        Position position = new Position();
        position.setPositionId(1L);

        when(positionRepository.findAllEffectiveAt(any()))
                .thenReturn(List.of(position));

        assertThat(
                positionService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1)))
                .hasSize(1);
    }

    @Test
    @DisplayName("役職一覧が空の場合はBusinessException")
    void findAllEffectiveAtTest2() {

        when(positionRepository.findAllEffectiveAt(any()))
                .thenReturn(Collections.emptyList());

        when(messageSource.getMessage(any(), any(), any()))
                .thenReturn("対象データが存在しません");

        assertThatThrownBy(
                () -> positionService.findAllEffectiveAt(
                        LocalDate.of(2026, 7, 1)))
                .isInstanceOf(BusinessException.class);
    }
}