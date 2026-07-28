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
import com.example.backend.entity.Department;
import com.example.backend.entity.Employee;
import com.example.backend.entity.SalaryResult;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final MessageSource messageSource;
    private final EmployeeRepository employeeRepository;
    private final SalaryResultRepository salaryResultRepository;

    /** 対象日時点で有効な部署を取得する. */
    @Transactional
    public Department findEffectiveAt(Long departmentId, LocalDate targetDate) {
        Department department = departmentRepository.findEffectiveAt(departmentId, targetDate);
        if (department == null) {
            throw MasterException("master.find.notfound");
        } else {
            return department;
        }
    }

    /** 対象日時点で有効な部署リストを取得する. */
    @Transactional
    public List<Department> findAllEffectiveAt(LocalDate targetDate) {
        List<Department> departments = departmentRepository.findAllEffectiveAt(targetDate);
        if (departments.isEmpty()) {
            throw MasterException("master.find.notfound");
        } else {
            return departments;
        }
    }

    /** 過去分を含めて全件取得する. */
    @Transactional
    public List<Department> findAll() {
        List<Department> departments = departmentRepository.selectList(null);
        return departments;
    }

    /** ある部署における新しい部署データを登録. */
    @Transactional
    public Department updateDepartment(
            Long departmentId, LocalDate startDate, String departmentName) {

        // 現時点で最も開始日が先の部署レコードを取得し、開始日を切り替え日の前日に設定
        Department latestDepartment = departmentRepository.findLatestByDepartmentId(departmentId);
        //廃止予定の場合は変更不可にし、例外を返す
        if (latestDepartment.getEndDate() != null) {
            throw MasterException("scr100.delete.tobeInvalid");
        } else if (!latestDepartment.getStartDate().isBefore(startDate)) {
            throw MasterException("scr100.startDate.mustBeAfterCurrent");
        }
        latestDepartment.setEndDate(startDate.minusDays(1));
        int updateCount = departmentRepository.updateEndDate(latestDepartment);
        if (updateCount != 1) {
            throw MasterException("scr100.update.failure");
        }

        // 引数から新たな部署情報を作成
        Department newDepartment = new Department();
        newDepartment.setDepartmentId(departmentId);
        newDepartment.setStartDate(startDate);
        newDepartment.setDepartmentName(departmentName);
        newDepartment.setEndDate(null);
        departmentRepository.insert(newDepartment);
        return newDepartment;
    }

    /** 新しい部署を追加. */
    @Transactional
    public Department createDepartment(
            String departmentName,
            LocalDate startDate) {
        // 部署IDの最大値を取得し、最大値 + 1を設定して保存
        Department newDepartment = new Department();
        Long maxId = departmentRepository.findMaxId();
        newDepartment.setDepartmentId(maxId == null ? 1L : maxId + 1);
        newDepartment.setDepartmentName(departmentName);
        newDepartment.setStartDate(startDate);
        newDepartment.setEndDate(null);
        departmentRepository.insert(newDepartment);
        return newDepartment;
    }

    /** 誤動作で削除されたデータを削除 */
    @Transactional
    public void deleteDepartment(Long departmentId, LocalDate startDate) {
        // 社員、給与実績に紐づいている場合は、削除せずに例外を返す.
        LambdaQueryWrapper<Employee> empWrapper = Wrappers.lambdaQuery();
        empWrapper.eq(Employee::getDepartmentId, departmentId);
        long empCount = employeeRepository.selectCount(empWrapper);
        LambdaQueryWrapper<SalaryResult> salaryWrapper = Wrappers.lambdaQuery();
        salaryWrapper.eq(SalaryResult::getDepartmentId, departmentId);
        long salaryCount = salaryResultRepository.selectCount(salaryWrapper);
        if (empCount != 0 || salaryCount != 0) {
            throw MasterException("scr100.delete.inUse");
        }

        // 削除対象取得.
        Department department = departmentRepository.findEffectiveAt(departmentId, startDate);
        if (department.getStartDate().isBefore(LocalDate.now())
            || department.getStartDate().isEqual(LocalDate.now())) {
                throw MasterException("scr100.delete.historyNotAllowed");
        } else if (department.getEndDate() != null) {
            throw MasterException("scr100.delete.hasNext");
        }
        departmentRepository.deleteDepartment(department);
    }

    /** 共通の例外を返すメソッド. */
    public BusinessException MasterException(String message) {
        return new BusinessException(
                messageSource.getMessage(
                        message, null, Locale.getDefault()));
    }
}