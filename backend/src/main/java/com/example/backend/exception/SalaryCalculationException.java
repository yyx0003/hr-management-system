package com.example.backend.exception;

import org.springframework.http.HttpStatus;

import com.example.backend.common.exception.BusinessException;

/**
 * 給与計算処理中の業務データ不整合を表す例外。
 * 発生時は当該社員のみ処理をスキップし、全体の処理は継続する。
 *
 * チームの統一異常処理（BusinessException／GlobalExceptionHandler）に合わせるため、
 * BusinessExceptionを継承する。マスタデータが見つからないケースが中心のため、
 * デフォルトのHttpStatusはNOT_FOUNDとする。
 */
public class SalaryCalculationException extends BusinessException {

    public SalaryCalculationException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public SalaryCalculationException(HttpStatus status, String message) {
        super(status, message);
    }
}