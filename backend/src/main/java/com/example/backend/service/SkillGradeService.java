package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.SkillGrade;
import com.example.backend.repository.SkillGradeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillGradeService {

    private final SkillGradeRepository skillGradeRepository;
    private final MessageSource messageSource;

    /** 対象日時点で有効な職能資格情報を取得する. */
    @Transactional
    public SkillGrade findEffectiveAt(
            Integer skillGrade,
            LocalDate targetDate) {

        SkillGrade skillGradeEntity = skillGradeRepository.findEffectiveAt(
                skillGrade,
                targetDate);

        if (skillGradeEntity == null) {
            throw new BusinessException(
                    messageSource.getMessage(
                            "master.find.notfound",
                            null,
                            Locale.getDefault()));
        }

        return skillGradeEntity;
    }

    /** 対象日時点で有効な職能資格情報リストを取得する. */
    @Transactional(readOnly = true)
    public List<SkillGrade> findAllEffectiveAt(
            LocalDate targetDate) {

        List<SkillGrade> skillGrades = skillGradeRepository.findAllEffectiveAt(
                targetDate);

        if (skillGrades.isEmpty()) {
            throw new BusinessException(
                    messageSource.getMessage(
                            "master.find.notfound",
                            null,
                            Locale.getDefault()));
        }

        return skillGrades;
    }
}