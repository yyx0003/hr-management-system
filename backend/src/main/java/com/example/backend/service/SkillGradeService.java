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
import com.example.backend.entity.SkillGrade;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SkillGradeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillGradeService {

        private final SkillGradeRepository skillGradeRepository;
        private final EmployeeRepository employeeRepository;
        private final MessageSource messageSource;

        /** 全履歴取得 */
        @Transactional
        public List<SkillGrade> findAll() {
                return skillGradeRepository.selectList(null);
        }

        /** 対象IDの日付時点での履歴を取得 */
        @Transactional
        public SkillGrade findEffectiveAt(Integer skillGrade, LocalDate targetDate) {
                SkillGrade skillGradeEntity = skillGradeRepository.findEffectiveAt(skillGrade, targetDate);
                if (skillGradeEntity == null) {
                        throw MasterException("master.find.notfound");
                } else {
                        return skillGradeEntity;
                }
        }

        /** 対象日時点で有効な職能資格を取得 */
        @Transactional
        public List<SkillGrade> findAllEffectiveAt(LocalDate targetDate) {
                return skillGradeRepository.findAllEffectiveAt(targetDate);           
        }

        /** 職能資格履歴更新 */
        @Transactional
        public SkillGrade updateSkillGrade(
                        Integer skillGrade,
                        Long allowance,
                        LocalDate startDate) {

                SkillGrade latestSkillGrade = skillGradeRepository
                                .findLatestBySkillGrade(skillGrade);

                if (latestSkillGrade.getEndDate() != null) {
                        throw MasterException("scr100.delete.tobeInvalid");
                } else if (!latestSkillGrade
                                .getStartDate()
                                .isBefore(startDate)) {

                        throw MasterException(
                                        "scr100.startDate.mustBeAfterCurrent");
                }

                latestSkillGrade.setEndDate(
                                startDate.minusDays(1));

                int updateCount = skillGradeRepository.updateEndDate(
                                latestSkillGrade);

                if (updateCount != 1) {
                        throw MasterException(
                                        "scr100.update.failure");
                }

                SkillGrade newSkillGrade = new SkillGrade();
                newSkillGrade.setSkillGrade(skillGrade);
                newSkillGrade.setAllowance(allowance);
                newSkillGrade.setStartDate(startDate);
                newSkillGrade.setEndDate(null);
                skillGradeRepository.insert(newSkillGrade);
                return newSkillGrade;
        }

        /** 職能資格履歴削除 */
        @Transactional
        public void deleteSkillGrade(
                        Integer skillGrade,
                        LocalDate startDate) {

                LambdaQueryWrapper<Employee> empWrapper = Wrappers.lambdaQuery();

                empWrapper.eq(Employee::getSkillGrade, skillGrade);

                long empCount = employeeRepository.selectCount(empWrapper);

                if (empCount != 0) {
                        throw MasterException(
                                        "scr100.delete.inUse");
                }

                SkillGrade skillGradeEntity = skillGradeRepository.findEffectiveAt(
                                skillGrade,
                                startDate);

                if (skillGradeEntity == null) {
                        throw MasterException("master.find.notfound");
                }

                if (skillGradeEntity.getStartDate()
                                .isBefore(LocalDate.now())
                                || skillGradeEntity.getStartDate()
                                                .isEqual(LocalDate.now())) {

                        throw MasterException(
                                        "scr100.delete.historyNotAllowed");
                } else if (skillGradeEntity.getEndDate() != null) {
                        throw MasterException("scr100.delete.hasNext");
                }

                int deleteCount = skillGradeRepository.deleteSkillGrade(
                                skillGradeEntity);

                if (deleteCount != 1) {
                        throw MasterException(
                                        "scr100.delete.failure");
                }

                SkillGrade latestSkillGrade = skillGradeRepository.findLatestBySkillGrade(skillGrade);

                if (latestSkillGrade != null) {
                        latestSkillGrade.setEndDate(null);
                        skillGradeRepository.updateEndDate(latestSkillGrade);
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