package com.example.backend.dto.attendance;

/**
 * 月次勤怠削除結果。
 *
 * @param deletedCount 削除件数
 * @param message 結果メッセージ
 */
public record AttendanceMonthlyDeleteResponse(
        int deletedCount,
        String message) {
}
