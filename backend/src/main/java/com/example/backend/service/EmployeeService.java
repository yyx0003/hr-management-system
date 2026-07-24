package com.example.backend.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.employee.EmployeeCreateRequest;
import com.example.backend.dto.employee.EmployeeCreateResponse;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.dto.employee.EmployeeQualificationCreateRequest;
import com.example.backend.dto.employee.EmployeeQualificationUpdateRequest;
import com.example.backend.dto.employee.EmployeeUpdateRequest;
import com.example.backend.dto.employee.EmployeeUpdateResponse;
import com.example.backend.dto.employee.QualificationDetailDTO;
import com.example.backend.entity.Employee;
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

// ---- 胡追加分ここから ----
import java.time.YearMonth;
import java.util.List;
// ---- 胡追加分ここまで ----

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
    private final MessageService messageService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final SkillGradeService skillGradeService;
    private final QualificationService qualificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeCreateResponse createEmployee(EmployeeCreateRequest request) {
        validateRetireDate(request);
        List<EmployeeQualificationCreateRequest> qualifications = request.qualifications() == null
                ? Collections.emptyList()
                : request.qualifications();
        validateDuplicateQualifications(qualifications);

        departmentService.findEffectiveAt(request.departmentId(), request.hireDate());
        skillGradeService.findEffectiveAt(request.skillGrade(), request.hireDate());
        if (request.positionId() != null) {
            positionService.findEffectiveAt(request.positionId(), request.hireDate());
        }
        for (EmployeeQualificationCreateRequest qualification : qualifications) {
            qualificationService.findEffectiveAt(qualification.qualificationId(), request.hireDate());
        }

        String employeeNo = nextEmployeeNo();
        Employee employee = new Employee();
        employee.setStartDate(request.hireDate());
        employee.setEmployeeNo(employeeNo);
        employee.setPasswordHash(passwordEncoder.encode(employeeNo));
        employee.setEmployeeName(request.employeeName());
        employee.setBirthDate(request.birthDate());
        employee.setPostalCode(request.postalCode());
        employee.setAddress(request.address());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setEmailAddress(request.emailAddress());
        employee.setHireDate(request.hireDate());
        employee.setRetireDate(request.retireDate());
        employee.setDepartmentId(request.departmentId());
        employee.setSkillGrade(request.skillGrade());
        employee.setPositionId(request.positionId());
        employee.setEndDate(null);
        employeeRepository.insert(employee);

        for (EmployeeQualificationCreateRequest qualification : qualifications) {
            EmployeeQualification employeeQualification = new EmployeeQualification();
            employeeQualification.setEmployeeId(employee.getEmployeeId());
            employeeQualification.setQualificationId(qualification.qualificationId());
            employeeQualification.setAcquisitionDate(qualification.acquisitionDate());
            employeeQualificationRepository.insert(employeeQualification);
        }

        return new EmployeeCreateResponse(employee.getEmployeeId(), employeeNo);
    }

    @Transactional
    public EmployeeUpdateResponse updateEmployee(String employeeNo, EmployeeUpdateRequest request) {
        LocalDate updateDate = LocalDate.now();
        LocalDate nextMonthStart = updateDate.withDayOfMonth(1).plusMonths(1);
        Employee current = employeeRepository
                .findEffectiveAndEmployedByEmployeeNoAtForUpdate(employeeNo, updateDate);
        if (current == null) {
            throw employeeNotFound(employeeNo);
        }

        validateRetireDate(request.retireDate(), current.getHireDate());
        List<EmployeeQualificationUpdateRequest> qualifications = request.qualifications() == null
                ? Collections.emptyList()
                : request.qualifications();
        validateDuplicateUpdateQualifications(qualifications);
        validateUpdateMasters(request, qualifications, nextMonthStart, updateDate);

        Employee scheduled = employeeRepository.findByEmployeeNoAndStartDate(employeeNo, nextMonthStart);
        updatePersonalInfo(current, request);
        if (scheduled != null) {
            updateScheduledHistory(scheduled, request);
        } else if (organizationChanged(current, request)) {
            int updated = employeeRepository.updateEndDate(
                    current.getEmployeeId(), current.getStartDate(), nextMonthStart.minusDays(1));
            requireOneEmployeeHistoryUpdate(updated);
            requireOneEmployeeHistoryUpdate(
                    employeeRepository.insert(createNextHistory(current, request, nextMonthStart)));
        }

        if (employeeRepository.updateRetireDateByEmployeeId(current.getEmployeeId(), request.retireDate()) <= 0) {
            throw employeeUpdateConflict();
        }
        synchronizeQualifications(current.getEmployeeId(), qualifications);
        return new EmployeeUpdateResponse(current.getEmployeeId(), current.getEmployeeNo());
    }

    public java.util.List<EmployeeListDTO> searchEmployees(
            String employeeNo, String employeeName, Long departmentId) {
        return employeeRepository.searchEffectiveAndEmployed(
                escapeLike(employeeNo),
                escapeLike(employeeName),
                departmentId,
                LocalDate.now());
    }

    public EmployeeDetailDTO getEmployeeDetail(String employeeNo) {
        Employee employee = getEffectiveEmployee(employeeNo, LocalDate.now());

        var qualifications = employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(employee.getEmployeeId())
                .stream()
                .map(qualification -> new QualificationDetailDTO(
                        qualification.getQualificationId(),
                        qualification.getAcquisitionDate()))
                .toList();

        return new EmployeeDetailDTO(
                employee.getEmployeeId(),
                employee.getEmployeeNo(),
                employee.getEmployeeName(),
                employee.getDepartmentId(),
                employee.getBirthDate(),
                employee.getPostalCode(),
                employee.getAddress(),
                employee.getPhoneNumber(),
                employee.getEmailAddress(),
                employee.getHireDate(),
                employee.getRetireDate(),
                employee.getPositionId(),
                employee.getSkillGrade(),
                qualifications);
    }

    public Employee getEffectiveEmployee(String employeeNo, LocalDate referenceDate) {
        Employee employee = employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(
                employeeNo,
                referenceDate);

        if (employee == null) {
            throw employeeNotFound(employeeNo);
        }

        return employee;
    }
    // ==========================================================
    // 胡追加分ここから（給与計算・退職者削除バッチ用）
    // ==========================================================

    /**
     * 対象年月に在籍していた社員のうち、salary_resultが未登録の社員IDを取得する（給与計算対象）。
     * 再実行時は既に登録済みの社員が自動的に除外されるため、そのまま「補完実行」としても使える。
     */
    public List<Long> findEligibleEmployeeIds(YearMonth targetYearMonth) {
        LocalDate targetMonthStart = targetYearMonth.atDay(1);
        LocalDate targetMonthEnd = targetYearMonth.atEndOfMonth();
        return employeeRepository.findEligibleEmployeeIds(
                targetMonthStart, targetMonthEnd, targetYearMonth.getYear(), targetYearMonth.getMonthValue());
    }

    /** 削除基準日以前に退職した社員のIDを、重複を除いて取得する（退職者削除バッチ対象）。 */
    public List<Long> findRetireeIds(LocalDate cutoffDate) {
        return employeeRepository.findRetireeIds(cutoffDate);
    }

    /**
     * 対象社員の対象年月末日時点で有効なemployeeレコードを取得する。
     * この1行にdepartmentId・skillGrade・positionIdがまとめて含まれる。
     */
    public Employee findEffectiveEmployeeAt(Long employeeId, LocalDate targetMonthEnd) {
        return employeeRepository.findEffectiveAt(employeeId, targetMonthEnd);
    }

    /** 対象employeeIdの全履歴行を削除する（退職者削除バッチで使用）。 */
    public void deleteByEmployeeId(Long employeeId) {
        employeeRepository.deleteByEmployeeId(employeeId);
    }

    // ==========================================================
    // 胡追加分ここまで
    // ==========================================================

    private String escapeLike(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private String nextEmployeeNo() {
        long sequenceValue = employeeRepository.nextEmployeeNoSequenceValue();
        if (sequenceValue > 9999) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    messageService.getMessage("error.employee.number.exhausted"));
        }
        return String.format("%04d", sequenceValue);
    }

    private void validateDuplicateQualifications(
            List<EmployeeQualificationCreateRequest> qualifications) {
        Set<Long> qualificationIds = new HashSet<>();
        for (EmployeeQualificationCreateRequest qualification : qualifications) {
            if (!qualificationIds.add(qualification.qualificationId())) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        messageService.getMessage(
                                "error.employee.qualification.duplicate",
                                qualification.qualificationId()));
            }
        }
    }

    private void validateDuplicateUpdateQualifications(
            List<EmployeeQualificationUpdateRequest> qualifications) {
        Set<Long> qualificationIds = new HashSet<>();
        for (EmployeeQualificationUpdateRequest qualification : qualifications) {
            if (!qualificationIds.add(qualification.qualificationId())) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        messageService.getMessage(
                                "error.employee.qualification.duplicate",
                                qualification.qualificationId()));
            }
        }
    }

    private void validateRetireDate(EmployeeCreateRequest request) {
        validateRetireDate(request.retireDate(), request.hireDate());
    }

    private void validateRetireDate(LocalDate retireDate, LocalDate hireDate) {
        if (retireDate != null && retireDate.isBefore(hireDate)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    messageService.getMessage("error.employee.retireDate.beforeHireDate"));
        }
    }

    private void validateUpdateMasters(EmployeeUpdateRequest request,
                                       List<EmployeeQualificationUpdateRequest> qualifications,
                                       LocalDate nextMonthStart,
                                       LocalDate updateDate) {
        departmentService.findEffectiveAt(request.departmentId(), nextMonthStart);
        skillGradeService.findEffectiveAt(request.skillGrade(), nextMonthStart);
        if (request.positionId() != null) {
            positionService.findEffectiveAt(request.positionId(), nextMonthStart);
        }
        for (EmployeeQualificationUpdateRequest qualification : qualifications) {
            qualificationService.findEffectiveAt(qualification.qualificationId(), updateDate);
        }
    }

    private void updatePersonalInfo(Employee employee, EmployeeUpdateRequest request) {
        requireOneEmployeeHistoryUpdate(employeeRepository.updatePersonalInfo(employee.getEmployeeId(), employee.getStartDate(),
                request.employeeName(), request.birthDate(), request.postalCode(), request.address(),
                request.phoneNumber(), request.emailAddress()));
    }

    private void updateScheduledHistory(Employee employee, EmployeeUpdateRequest request) {
        requireOneEmployeeHistoryUpdate(employeeRepository.updateScheduledHistory(employee.getEmployeeId(), employee.getStartDate(),
                request.employeeName(), request.birthDate(), request.postalCode(), request.address(),
                request.phoneNumber(), request.emailAddress(), request.departmentId(), request.positionId(),
                request.skillGrade()));
    }

    private boolean organizationChanged(Employee current, EmployeeUpdateRequest request) {
        return !Objects.equals(current.getDepartmentId(), request.departmentId())
                || !Objects.equals(current.getPositionId(), request.positionId())
                || !Objects.equals(current.getSkillGrade(), request.skillGrade());
    }

    private Employee createNextHistory(Employee current, EmployeeUpdateRequest request,
                                       LocalDate nextMonthStart) {
        Employee next = new Employee();
        next.setEmployeeId(current.getEmployeeId());
        next.setStartDate(nextMonthStart);
        next.setEmployeeNo(current.getEmployeeNo());
        next.setPasswordHash(current.getPasswordHash());
        next.setEmployeeName(request.employeeName());
        next.setBirthDate(request.birthDate());
        next.setPostalCode(request.postalCode());
        next.setAddress(request.address());
        next.setPhoneNumber(request.phoneNumber());
        next.setEmailAddress(request.emailAddress());
        next.setHireDate(current.getHireDate());
        next.setRetireDate(request.retireDate());
        next.setDepartmentId(request.departmentId());
        next.setPositionId(request.positionId());
        next.setSkillGrade(request.skillGrade());
        next.setEndDate(null);
        return next;
    }

    private void synchronizeQualifications(Long employeeId,
                                            List<EmployeeQualificationUpdateRequest> requestedQualifications) {
        Map<Long, EmployeeQualification> existingById = new HashMap<>();
        for (EmployeeQualification existing :
                employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(employeeId)) {
            existingById.put(existing.getQualificationId(), existing);
        }

        Set<Long> requestedIds = new HashSet<>();
        for (EmployeeQualificationUpdateRequest requested : requestedQualifications) {
            requestedIds.add(requested.qualificationId());
        }
        for (Long existingId : existingById.keySet()) {
            if (!requestedIds.contains(existingId)) {
                requireOneQualificationUpdate(employeeQualificationRepository
                        .deleteByEmployeeIdAndQualificationId(employeeId, existingId));
            }
        }
        for (EmployeeQualificationUpdateRequest requested : requestedQualifications) {
            EmployeeQualification existing = existingById.get(requested.qualificationId());
            if (existing == null) {
                EmployeeQualification qualification = new EmployeeQualification();
                qualification.setEmployeeId(employeeId);
                qualification.setQualificationId(requested.qualificationId());
                qualification.setAcquisitionDate(requested.acquisitionDate());
                requireOneQualificationUpdate(employeeQualificationRepository.insert(qualification));
            } else if (!Objects.equals(existing.getAcquisitionDate(), requested.acquisitionDate())) {
                requireOneQualificationUpdate(employeeQualificationRepository.updateAcquisitionDate(
                        employeeId, requested.qualificationId(), requested.acquisitionDate()));
            }
        }
    }

    private void requireOneEmployeeHistoryUpdate(int updatedRows) {
        if (updatedRows != 1) {
            throw employeeUpdateConflict();
        }
    }

    private void requireOneQualificationUpdate(int updatedRows) {
        if (updatedRows != 1) {
            throw employeeQualificationUpdateConflict();
        }
    }

    private BusinessException employeeUpdateConflict() {
        return new BusinessException(HttpStatus.CONFLICT,
                messageService.getMessage("error.employee.update.conflict"));
    }

    private BusinessException employeeQualificationUpdateConflict() {
        return new BusinessException(HttpStatus.CONFLICT,
                messageService.getMessage("error.employee.qualification.update.conflict"));
    }

    private BusinessException employeeNotFound(String employeeNo) {
        return new BusinessException(HttpStatus.NOT_FOUND,
                messageService.getMessage("error.employee.notfound", employeeNo));
    }
}
