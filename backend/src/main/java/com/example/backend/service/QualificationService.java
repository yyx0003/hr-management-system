package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        try {
            Qualification qualification = qualificationRepository.findEffectiveAt(qualificationId, targetDate);
            if (qualification == null) {
                throw new NoSuchElementException(
                        messageSource.getMessage("master.find.notfound", null, Locale.getDefault()));
            } else {
                return qualification;
            }
        } catch (TooManyResultsException e) {
            // 有効なレコードが重複している場合はエラーを返す.
            throw new TooManyResultsException(
                    messageSource.getMessage("master.find.duplicate", null, Locale.getDefault()));
        }
    }

    /** 対象日時点で有効な資格情報リストを取得する. */
    @Transactional
    public List<Qualification> findAllEffectiveAt(LocalDate targetDate) {
        List<Qualification> qualifications = qualificationRepository.findAllEffectiveAt(targetDate);
        if (qualifications.isEmpty()) {
            throw new NoSuchElementException(
                    messageSource.getMessage("master.find.notfound", null, Locale.getDefault()));
        } else {
            return qualifications;
        }
    }
}
