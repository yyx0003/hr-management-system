package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 退職者データ削除のドメインロジックを提供するサービス。
 *
 * employee_idを参照する外部キー制約は設定していないため（employeeが複合主キーのため）、
 * ON DELETE CASCADEには依存できない。子テーブルを1つずつ手動で削除し、
 * 最後にemployee本体（該当employee_idの全履歴行）を削除する。
 *
 * qualification・positionのマスタ変更履歴（qualification／position自体の複合主キー行）は
 * 社員に紐づかないため、本処理の削除対象に含めない。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetireeDeleteService {

    private final SalaryResultRepository salaryResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * 対象社員のデータを削除する（1社員単位のトランザクション）。
     * 削除順序：salary_result → attendance → employee_qualification → employee
     * （employeeの削除は、start_dateを条件に含めないため該当employee_idの全履歴行が対象となる）。
     */
    @Transactional
    public void deleteEmployeeData(Long employeeId) {
        salaryResultRepository.deleteByEmployeeId(employeeId);
        attendanceRepository.deleteByEmployeeId(employeeId);
        employeeQualificationRepository.deleteByEmployeeId(employeeId);
        employeeRepository.deleteByEmployeeId(employeeId);
        log.info("社員データを削除しました。employeeId={}", employeeId);
    }
}