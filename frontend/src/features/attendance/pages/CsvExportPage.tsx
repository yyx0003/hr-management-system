import { useState } from 'react'
import {
  exportHrCsv,
  exportManagementCsv,
  getAttendanceApiErrorMessage,
} from '../api'
import {
  CSV_EXPORT_TYPE,
  type CsvExportType,
} from '../constants'
import { ATTENDANCE_MESSAGES } from '../messages'
import { getCurrentMonth } from '../utils'
import '../attendance.css'

export function CsvExportPage() {
  const [exportType, setExportType] =
    useState<CsvExportType>(CSV_EXPORT_TYPE.hr)
  const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
  const [exporting, setExporting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleExport = async () => {
    setExporting(true)
    setErrorMessage('')

    try {
      if (exportType === CSV_EXPORT_TYPE.hr) {
        await exportHrCsv(targetMonth)
      } else {
        await exportManagementCsv(targetMonth)
      }
    } catch (error: unknown) {
      setErrorMessage(
        getAttendanceApiErrorMessage(
          error,
          ATTENDANCE_MESSAGES.csvExportFailed,
        ),
      )
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="attendance-page">
      <div className="attendance-page-header">
        <div>
          <p className="attendance-page-eyebrow">
            CSV EXPORT
          </p>
          <h2>CSV出力</h2>
          <p className="attendance-page-description">
            対象年月を指定して人事向けまたは経営向けのCSVを出力します。
          </p>
        </div>
      </div>

      <section className="attendance-card attendance-export-card">
        <div
          aria-label="CSV出力種別"
          className="attendance-tabs"
          role="tablist"
        >
          <button
            aria-selected={exportType === CSV_EXPORT_TYPE.hr}
            className={
              exportType === CSV_EXPORT_TYPE.hr
                ? 'attendance-tab attendance-tab-active'
                : 'attendance-tab'
            }
            role="tab"
            type="button"
            onClick={() =>
              setExportType(CSV_EXPORT_TYPE.hr)
            }
          >
            人事向けCSV
          </button>

          <button
            aria-selected={
              exportType === CSV_EXPORT_TYPE.management
            }
            className={
              exportType === CSV_EXPORT_TYPE.management
                ? 'attendance-tab attendance-tab-active'
                : 'attendance-tab'
            }
            role="tab"
            type="button"
            onClick={() =>
              setExportType(CSV_EXPORT_TYPE.management)
            }
          >
            経営向けCSV
          </button>
        </div>

        <div className="attendance-export-content">
          <div>
            <span className="attendance-export-badge">
              {exportType === CSV_EXPORT_TYPE.hr
                ? '人事向け'
                : '経営向け'}
            </span>

            <h3>
              {exportType === CSV_EXPORT_TYPE.hr
                ? '全従業員の給与・勤怠情報'
                : '部署別の給与・稼働集計'}
            </h3>

            <p>
              {exportType === CSV_EXPORT_TYPE.hr
                ? '全従業員の稼働時間、残業時間、給与情報をCSV形式で出力します。'
                : '部署ごとの所属人数、給与総額、稼働時間、残業時間をCSV形式で出力します。'}
            </p>
          </div>

          <label className="attendance-form-field">
            <span>対象年月</span>
            <input
              type="month"
              value={targetMonth}
              onChange={(event) =>
                setTargetMonth(event.target.value)
              }
            />
          </label>

          {errorMessage && (
            <div
              className="attendance-alert attendance-alert-error"
              role="alert"
            >
              {errorMessage}
            </div>
          )}

          <div className="attendance-import-actions">
            <button
              className="attendance-primary-button"
              disabled={exporting}
              type="button"
              onClick={() => void handleExport()}
            >
              {exporting ? '出力中...' : 'CSVを出力する'}
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}