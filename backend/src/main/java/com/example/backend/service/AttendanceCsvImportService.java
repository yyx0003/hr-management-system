package com.example.backend.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.common.MessageService;
import com.example.backend.dto.attendance.AttendanceCsvImportResponse;
import com.example.backend.dto.attendance.AttendanceCsvParseResult;
import com.example.backend.dto.attendance.AttendanceCsvValidatedRow;
import com.example.backend.dto.attendance.AttendanceCsvValidationResult;
import com.example.backend.dto.attendance.CsvImportError;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠CSVの取込処理を行うサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceCsvImportService {

    private final AttendanceCsvParserService
            attendanceCsvParserService;

    private final AttendanceCsvValidationService
            attendanceCsvValidationService;

    private final AttendanceRepository
            attendanceRepository;

    private final MessageService messageService;

    /**
     * 勤怠CSVを取り込む。
     *
     * CSV内に1件でもエラーがある場合、
     * データベースは更新しない。
     *
     * 全件正常な場合、
     * 対象社員・対象年月の既存勤怠を削除し、
     * CSVの内容で置き換える。
     *
     * 給与計算およびsalary_resultの更新は行わない。
     *
     * @param employeeId 社員ID
     * @param employeeNo 社員番号
     * @param targetMonth 取込対象年月
     * @param file CSVファイル
     * @return CSV取込結果
     */
    @Transactional
    public AttendanceCsvImportResponse importCsv(
            Long employeeId,
            String employeeNo,
            YearMonth targetMonth,
            MultipartFile file) {

        validateArguments(
                employeeId,
                employeeNo,
                targetMonth);

        /*
         * CSVファイルを読み込む。
         */
        AttendanceCsvParseResult parseResult =
                attendanceCsvParserService.parse(file);

        /*
         * 正常に読み込めた行を全件確認する。
         */
        AttendanceCsvValidationResult validationResult =
                attendanceCsvValidationService.validate(
                        parseResult.getRows(),
                        targetMonth,
                        employeeNo);

        /*
         * CSV形式エラーと業務エラーをまとめる。
         */
        List<CsvImportError> errors =
                new ArrayList<>();

        errors.addAll(parseResult.getErrors());
        errors.addAll(validationResult.getErrors());

        /*
         * 1件でもエラーがある場合は、
         * 削除・登録を行わずに結果を返す。
         */
        if (!errors.isEmpty()) {
            return createErrorResponse(errors);
        }

        /*
         * 対象社員・対象年月の既存勤怠を削除する。
         */
        attendanceRepository
                .deleteByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetMonth.atDay(1),
                        targetMonth.atEndOfMonth());

        /*
         * CSVの勤怠データを登録する。
         */
        int registeredCount =
                registerAttendances(
                        employeeId,
                        validationResult.getRows());

        return createSuccessResponse(
                parseResult.getRows().size(),
                registeredCount);
    }

    /**
     * 引数を確認する。
     *
     * @param employeeId 社員ID
     * @param employeeNo 社員番号
     * @param targetMonth 対象年月
     */
    private void validateArguments(
            Long employeeId,
            String employeeNo,
            YearMonth targetMonth) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "employeeId must not be null");
        }

        if (employeeNo == null
                || employeeNo.isBlank()) {

            throw new IllegalArgumentException(
                    "employeeNo must not be blank");
        }

        if (targetMonth == null) {
            throw new IllegalArgumentException(
                    "targetMonth must not be null");
        }
    }

    /**
     * CSVの勤怠データを登録する。
     *
     * @param employeeId 社員ID
     * @param rows 校验完成的数据
     * @return 登録件数
     */
    private int registerAttendances(
            Long employeeId,
            List<AttendanceCsvValidatedRow> rows) {

        int registeredCount = 0;

        for (AttendanceCsvValidatedRow row : rows) {

            Attendance attendance =
                    createAttendance(
                            employeeId,
                            row);

            attendanceRepository.insert(
                    attendance);

            registeredCount++;
        }

        return registeredCount;
    }

    /**
     * 登録用の勤怠Entityを作成する。
     *
     * @param employeeId 社員ID
     * @param row CSV数据
     * @return 勤怠Entity
     */
    private Attendance createAttendance(
            Long employeeId,
            AttendanceCsvValidatedRow row) {

        Attendance attendance =
                new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setWorkDate(
                row.getWorkDate());
        attendance.setAttendanceTime(
                row.getAttendanceTime());
        attendance.setLeavingTime(
                row.getLeavingTime());
        attendance.setWorkType(
                row.getWorkType());

        return attendance;
    }

    /**
     * エラー時の結果を作成する。
     *
     * @param errors エラー一覧
     * @return 取込結果
     */
    private AttendanceCsvImportResponse
            createErrorResponse(
                    List<CsvImportError> errors) {

        return new AttendanceCsvImportResponse(
                false,
                messageService.getMessage(
                        "scr070.import.hasErrors",
                        errors.size()),
                errors);
    }

    /**
     * 成功時の結果を作成する。
     *
     * @param readCount 読込件数
     * @param registeredCount 登録件数
     * @return 取込結果
     */
    private AttendanceCsvImportResponse
            createSuccessResponse(
                    int readCount,
                    int registeredCount) {

        return new AttendanceCsvImportResponse(
                true,
                messageService.getMessage(
                        "scr070.import.success",
                        readCount,
                        registeredCount),
                List.of());
    }
}