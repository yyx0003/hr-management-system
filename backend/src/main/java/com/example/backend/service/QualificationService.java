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
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.entity.Qualification;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.QualificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QualificationService {

    private final QualificationRepository qualificationRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
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

    /** 過去分を含めて全件取得する. */
    @Transactional
    public List<Qualification> findAll() {
        return qualificationRepository.selectList(null);
    }

    /** ある資格における新しい資格データを登録. */
    @Transactional
    public Qualification updateQualification(
            Long qualificationId,
            LocalDate startDate,
            String qualificationName,
            Boolean isAdvance,
            Long qualificationAllowance) {

        // 現時点で最も開始日が先の資格レコードを取得し、開始日を切り替え日の前日に設定
        Qualification latestQualification = qualificationRepository
                .findLatestByQualificationId(
                        qualificationId);

        // 廃止予定の場合は変更不可にし、例外を返す
        if (latestQualification.getEndDate() != null) {
            throw MasterException(
                    "scr100.delete.tobeInvalid");
        } else if (!latestQualification.getStartDate().isBefore(startDate)) {
            throw MasterException("scr100.startDate.mustBeAfterCurrent");
        }

        latestQualification.setEndDate(startDate.minusDays(1));
        int updateCount = qualificationRepository.updateEndDate(latestQualification);
        if (updateCount != 1) {
            throw MasterException("scr100.update.failure");
        }

        Qualification newQualification = new Qualification();
        newQualification.setQualificationId(qualificationId);
        newQualification.setQualificationName(qualificationName);
        newQualification.setIsAdvance(isAdvance);
        newQualification.setQualificationAllowance(qualificationAllowance);
        newQualification.setStartDate(startDate);
        newQualification.setEndDate(null);
        qualificationRepository.insert(newQualification);

        return newQualification;
    }

    /** 新しい資格を追加. */
    @Transactional
    public Qualification createQualification(
            String qualificationName,
            Boolean isAdvance,
            Long qualificationAllowance,
            LocalDate startDate) {

        Qualification newQualification = new Qualification();
        Long maxId = qualificationRepository.findMaxId();
        newQualification.setQualificationId(
                maxId == null ? 1L : maxId + 1);
        newQualification.setQualificationName(qualificationName);
        newQualification.setIsAdvance(isAdvance);
        newQualification.setQualificationAllowance(qualificationAllowance);
        newQualification.setStartDate(startDate);
        newQualification.setEndDate(null);
        qualificationRepository.insert(newQualification);

        return newQualification;
    }

    /** 資格履歴を削除. */
    @Transactional
    public void deleteQualification(
            Long qualificationId,
            LocalDate startDate) {

        //指定の資格を取得している社員がいる場合、例外を返す
        LambdaQueryWrapper<EmployeeQualification> empQualificationWrapper = Wrappers.lambdaQuery();
        empQualificationWrapper.eq(EmployeeQualification::getQualificationId, qualificationId);
        if (employeeQualificationRepository.selectCount(empQualificationWrapper) != 0) {
            throw MasterException("scr100.delete.inUse");
        }

        //削除対象取得
        Qualification qualification = qualificationRepository.findEffectiveAt(
                qualificationId,
                startDate);

        if (qualification == null) {
            throw MasterException("master.find.notfound");
        }
        if (qualification.getStartDate()
                .isBefore(LocalDate.now())
                || qualification.getStartDate()
                        .isEqual(LocalDate.now())) {

            throw MasterException(
                    "scr100.delete.historyNotAllowed");
        } else if (qualification.getEndDate() != null) {
            throw MasterException("scr100.delete.hasNext");
        }

        int deleteCount = qualificationRepository.deleteQualification(qualification);
        if (deleteCount != 1) {
            throw MasterException("scr100.delete.failure");
        }

        Qualification latestQualification = qualificationRepository.findLatestByQualificationId(qualificationId);
        if (latestQualification != null) {
            latestQualification.setEndDate(null);
            qualificationRepository.updateEndDate(latestQualification);
        }
    }

    /** 共通の例外を返すメソッド. */
    public BusinessException MasterException(String message) {
        return new BusinessException(
                messageSource.getMessage(
                        message, null, Locale.getDefault()));
    }
}
