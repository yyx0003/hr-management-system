package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Department;
import com.example.backend.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final MessageSource messageSource;

    /** 対象日時点で有効な部署を取得する. */
    @Transactional
    public Department findEffectiveAt(Long departmentId, LocalDate targetDate) {
        Department department = departmentRepository.findEffectiveAt(departmentId, targetDate);
        if (department == null) {
            throw new BusinessException(
                    messageSource.getMessage("master.find.notfound", null, Locale.getDefault()));
        } else {
            return department;
        }
    }

    /** 対象日時点で有効な部署リストを取得する. */
    @Transactional
    public List<Department> findAllEffectiveAt(LocalDate targetDate) {
        List<Department> departments = departmentRepository.findAllEffectiveAt(targetDate);
        if (departments.isEmpty()) {
            throw new BusinessException(
                    messageSource.getMessage("master.findEffectiveAt.notfound", null, Locale.getDefault()));
        } else {
            return departments;
        }
    }
}