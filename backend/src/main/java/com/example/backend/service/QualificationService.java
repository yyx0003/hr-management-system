package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Qualification;
import com.example.backend.repository.QualificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QualificationService {

    private final QualificationRepository qualificationRepository;
    private final MessageSource messageSource;

    /** 対象日時点で有効な資格情報を取得する. */
    @Transactional
    public Qualification findEffectiveAt(Long qualificationId, LocalDate targetDate) {
        Qualification qualification = qualificationRepository.findEffectiveAt(qualificationId, targetDate);
        if (qualification == null) {
            throw new BusinessException(
                    messageSource.getMessage("master.find.notfound", null, Locale.getDefault()));
        } else {
            return qualification;
        }
    }

    /** 対象日時点で有効な資格情報リストを取得する. */
    @Transactional
    public List<Qualification> findAllEffectiveAt(LocalDate targetDate) {
        List<Qualification> qualifications = qualificationRepository.findAllEffectiveAt(targetDate);
        if (qualifications.isEmpty()) {
            throw new BusinessException(
                    messageSource.getMessage("master.find.notfound", null, Locale.getDefault()));
        } else {
            return qualifications;
        }
    }
}
