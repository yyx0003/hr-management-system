package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Position;
import com.example.backend.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final MessageSource messageSource;

    /** 対象日時点で有効な役職情報を取得する. */
    @Transactional(readOnly = true)
    public Position findEffectiveAt(
            Long positionId,
            LocalDate targetDate) {

        Position position = positionRepository.findEffectiveAt(
                positionId,
                targetDate);

        if (position == null) {
            throw new BusinessException(
                    messageSource.getMessage(
                            "master.find.notfound",
                            null,
                            Locale.getDefault()));
        }

        return position;
    }

    /** 対象日時点で有効な役職情報リストを取得する. */
    @Transactional(readOnly = true)
    public List<Position> findAllEffectiveAt(
            LocalDate targetDate) {

        List<Position> positions = positionRepository.findAllEffectiveAt(
                targetDate);

        if (positions.isEmpty()) {
            throw new BusinessException(
                    messageSource.getMessage(
                            "master.find.notfound",
                            null,
                            Locale.getDefault()));
        }

        return positions;
    }
}