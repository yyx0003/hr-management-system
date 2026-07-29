import { useState } from 'react'
import {
  CSV_EXPORT_TYPE,
  type CsvExportType,
} from '../constants'
import { getCurrentMonth } from '../utils'
import '../attendance.css'

const EXPORT_DETAILS = {
  [CSV_EXPORT_TYPE.hr]: {
    title: '全従業員の給与・勤怠情報',
    description: '対象年月の全従業員データを出力します。',
    fileDescription:
      '稼働時間、残業時間、給与情報をCSV形式で出力します。',
    fileNamePrefix: '人事向け_勤怠給与',
  },
  [CSV_EXPORT_TYPE.management]: {
    title: '部署別の給与・稼働状況',
    description: '対象年月の部署別データを出力します。',
    fileDescription:
      '所属人数、給与総額、稼働時間合計、残業時間合計をCSV形式で出力します。',
    fileNamePrefix: '経営向け_部署別集計',
  },
} as const

export function CsvExportPage() {
  const [exportType, setExportType] =
    useState<CsvExportType>(CSV_EXPORT_TYPE.hr)
  const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
  const [exporting, setExporting] = useState(false)
  const [noticeMessage, setNoticeMessage] = useState('')

  const exportDetails = EXPORT_DETAILS[exportType]
  const targetMonthToken = targetMonth.replace('-', '')
  const fileName =
    `${exportDetails.fileNamePrefix}_${targetMonthToken}.csv`
  const formattedTargetMonth = formatTargetMonth(targetMonth)

  const selectExportType = (nextType: CsvExportType) => {
    setExportType(nextType)
    setNoticeMessage('')
  }

  const handleExport = async () => {
    if (exporting) return

    setExporting(true)
    setNoticeMessage('')

    // API接続までは画面状態のみ確認する。
    await new Promise((resolve) => window.setTimeout(resolve, 500))

    setNoticeMessage('この機能は現在準備中です。')
    setExporting(false)
  }

  return (
    <div className="attendance-page attendance-export-page">
      <div className="attendance-page-header">
        <div>
          <h2>CSV出力</h2>
          <p className="attendance-page-description">
            対象年月を指定して、勤怠・給与情報をCSV形式で出力します。
          </p>
        </div>
      </div>

      <section className="attendance-card attendance-export-card">
        <div className="attendance-export-inner">
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
              onClick={() => selectExportType(CSV_EXPORT_TYPE.hr)}
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
                selectExportType(CSV_EXPORT_TYPE.management)
              }
            >
              経営向けCSV
            </button>
          </div>

          <div className="attendance-export-content">
            <div className="attendance-export-heading">
              <h3>{exportDetails.title}</h3>
              <p>{exportDetails.description}</p>
            </div>

            <label className="attendance-form-field attendance-export-month-field">
              <span>対象年月</span>
              <input
                type="month"
                value={targetMonth}
                disabled={exporting}
                onChange={(event) => {
                  setTargetMonth(event.target.value)
                  setNoticeMessage('')
                }}
              />
            </label>

            <section className="attendance-export-file-card">
              <div className="attendance-export-file-title">
                <span className="attendance-export-csv-label">CSV</span>
                <h4>{fileName}</h4>
              </div>
              <p>{exportDetails.fileDescription}</p>

              <div className="attendance-export-file-footer">
                <div className="attendance-export-file-meta">
                  <span>形式：CSV</span>
                  <span>文字コード：UTF-8</span>
                  <span>対象年月：{formattedTargetMonth}</span>
                </div>

                <div className="attendance-import-actions attendance-export-actions">
                  <button
                    className="attendance-primary-button attendance-export-button"
                    disabled={exporting || !targetMonth}
                    type="button"
                    onClick={() => void handleExport()}
                  >
                    <span>
                      {exporting ? 'CSVを作成中...' : 'CSVを出力する'}
                    </span>
                    {exporting ? (
                      <span
                        aria-hidden="true"
                        className="attendance-export-spinner"
                      />
                    ) : (
                      <svg
                        aria-hidden="true"
                        className="attendance-export-download-icon"
                        viewBox="0 0 24 24"
                      >
                        <path d="M12 3v12" />
                        <path d="m8 11 4 4 4-4" />
                        <path d="M5 17v3h14v-3" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              {noticeMessage && (
                <div className="attendance-export-notice" role="status">
                  {noticeMessage}
                </div>
              )}
            </section>
          </div>
        </div>
      </section>
    </div>
  )
}

function formatTargetMonth(targetMonth: string): string {
  const [year, month] = targetMonth.split('-')
  if (!year || !month) return '―'
  return `${year}年${month}月`
}
