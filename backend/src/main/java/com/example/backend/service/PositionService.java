package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Employee;
import com.example.backend.entity.Position;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final MessageSource messageSource;

    /** 対象日時点で有効な役職を取得 */
    @Transactional
    public Position findEffectiveAt(
            Long positionId,
            LocalDate targetDate) {

        Position position =
                positionRepository.findEffectiveAt(
                        positionId,
                        targetDate);

        if (position == null) {
            throw MasterException("master.find.notfound");
        }

        return position;
    }

    /** 対象日時点で有効な役職一覧取得 */
    @Transactional
    public List<Position> findAllEffectiveAt(
            LocalDate targetDate) {

        List<Position> positions =
                positionRepository.findAllEffectiveAt(
                        targetDate);

        if (positions.isEmpty()) {
            throw MasterException("master.find.notfound");
        }

        return positions;
    }

    /** 全履歴取得 */
    @Transactional
    public List<Position> findAll() {
        return positionRepository.selectList(null);
    }

    /** 役職履歴更新 */
    @Transactional
    public Position updatePosition(
            Long positionId,
            LocalDate startDate,
            String positionName,
            Long positionAllowance) {

        Position latestPosition =
                positionRepository.findLatestByPositionId(
                        positionId);

        if (latestPosition.getEndDate() != null) {
            throw MasterException(
                    "scr100.delete.tobeInvalid");
        } else if (!latestPosition
                .getStartDate()
                .isBefore(startDate)) {

            throw MasterException(
                    "scr100.startDate.mustBeAfterCurrent");
        }

        latestPosition.setEndDate(
                startDate.minusDays(1));

        int updateCount =
                positionRepository.updateEndDate(
                        latestPosition);

        if (updateCount != 1) {
            throw MasterException(
                    "scr100.update.failure");
        }

        Position newPosition = new Position();

        newPosition.setPositionId(positionId);
        newPosition.setPositionName(positionName);
        newPosition.setPositionAllowance(positionAllowance);
        newPosition.setStartDate(startDate);
        newPosition.setEndDate(null);

        positionRepository.insert(newPosition);

        return newPosition;
    }

    /** 新しい役職を追加 */
    @Transactional
    public Position createPosition(
            String positionName,
            Long positionAllowance,
            LocalDate startDate) {

        Position newPosition = new Position();

        Long maxId = positionRepository.findMaxId();

        newPosition.setPositionId(
                maxId == null ? 1L : maxId + 1);

        newPosition.setPositionName(positionName);
        newPosition.setPositionAllowance(positionAllowance);
        newPosition.setStartDate(startDate);
        newPosition.setEndDate(null);

        positionRepository.insert(newPosition);

        return newPosition;
    }

    /** 役職履歴削除 */
    @Transactional
    public void deletePosition(
            Long positionId,
            LocalDate startDate) {

        // 他テーブル参照チェック
        LambdaQueryWrapper<Employee> empWrapper = Wrappers.lambdaQuery();
        empWrapper.eq(Employee::getPositionId, positionId);
        
        if (employeeRepository.selectCount(empWrapper) != 0) {
                throw MasterException("scr100.delete.inUse");
        }
        Position position = positionRepository.findEffectiveAt(
                        positionId,
                        startDate);

        if (position == null) {
            throw MasterException(
                    "master.find.notfound");
        }

        if (position.getStartDate()
                .isBefore(LocalDate.now())
                || position.getStartDate()
                        .isEqual(LocalDate.now())) {

            throw MasterException(
                    "scr100.delete.historyNotAllowed");
        } else if (position.getEndDate() != null) {
                throw MasterException("scr100.delete.hasNext");
        }

        int deleteCount = positionRepository.deletePosition(position);

        if (deleteCount != 1) {
            throw MasterException(
                    "scr100.delete.failure");
        }

        // 削除後に最も先のレコードの終了日をNULLにする
        Position latestPosition = positionRepository.findLatestByPositionId(positionId);
        if (latestPosition != null) {
                latestPosition.setEndDate(null);
                positionRepository.updateEndDate(latestPosition);
        }
    }

    /** 共通例外 */
    public BusinessException MasterException(
            String message) {

        return new BusinessException(
                messageSource.getMessage(
                        message,
                        null,
                        Locale.getDefault()));
    }
}