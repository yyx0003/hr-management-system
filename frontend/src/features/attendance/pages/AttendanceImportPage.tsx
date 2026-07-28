import { type ChangeEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../../constants/routes'
import {
  getAttendanceApiErrorMessage,
  importAttendanceCsv,
} from '../api'
import { ATTENDANCE_MESSAGES } from '../messages'
import type {
  AttendanceCsvImportResponse,
} from '../types'
import { getCurrentMonth } from '../utils'
import '../attendance.css'

export function AttendanceImportPage() {
  const navigate = useNavigate()
  const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] =
    useState<AttendanceCsvImportResponse | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [importing, setImporting] = useState(false)

  const handleFileChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const selectedFile = event.target.files?.[0] ?? null

    setResult(null)
    setErrorMessage('')

    if (
      selectedFile &&
      !selectedFile.name.toLowerCase().endsWith('.csv')
    ) {
      setFile(null)
      setErrorMessage(ATTENDANCE_MESSAGES.csvFileTypeInvalid)
      event.target.value = ''
      return
    }

    setFile(selectedFile)
  }

  const handleImport = async () => {
    if (!file) {
      setErrorMessage(ATTENDANCE_MESSAGES.csvFileRequired)
      return
    }

    setImporting(true)
    setResult(null)
    setErrorMessage('')

    try {
      const response = await importAttendanceCsv(
        targetMonth,
        file,
      )

      setResult(response)

      if (!response.success) {
        setErrorMessage(response.message)
      }
    } catch (error: unknown) {
      setErrorMessage(
        getAttendanceApiErrorMessage(
          error,
          ATTENDANCE_MESSAGES.csvImportFailed,
        ),
      )
    } finally {
      setImporting(false)
    }
  }

  return (
    <div className="attendance-page">
      <div className="attendance-page-header">
        <div>
          <p className="attendance-page-eyebrow">
            CSV IMPORT
          </p>
          <h2>勤怠CSV取込</h2>
          <p className="attendance-page-description">
            CSVファイルから1か月分の勤怠情報を一括登録します。
          </p>
        </div>

        <button
          className="attendance-secondary-button"
          type="button"
          onClick={() => navigate(ROUTES.attendances)}
        >
          勤怠管理へ戻る
        </button>
      </div>

      <section className="attendance-card attendance-import-card">
        <div className="attendance-import-warning">
          <strong>取込時の注意</strong>
          <p>
            CSV取込に成功した場合、対象月に既に登録されている勤怠情報は
            CSVの内容に置き換えられます。
          </p>
        </div>

        <div className="attendance-import-fields">
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

          <label className="attendance-form-field">
            <span>CSVファイル</span>
            <input
              accept=".csv,text/csv"
              type="file"
              onChange={handleFileChange}
            />
          </label>
        </div>

        {file && (
          <div className="attendance-selected-file">
            <span>選択中のファイル</span>
            <strong>{file.name}</strong>
            <small>{formatFileSize(file.size)}</small>
          </div>
        )}

        {errorMessage && (
          <div
            className="attendance-alert attendance-alert-error"
            role="alert"
          >
            {errorMessage}
          </div>
        )}

        {result?.success && (
          <div className="attendance-alert attendance-alert-success">
            {result.message ||
              ATTENDANCE_MESSAGES.csvImportSuccess}
          </div>
        )}

        <div className="attendance-import-actions">
          <button
            className="attendance-primary-button"
            disabled={importing}
            type="button"
            onClick={() => void handleImport()}
          >
            {importing ? '取込中...' : 'CSVを取り込む'}
          </button>
        </div>
      </section>

      {result && result.errors.length > 0 && (
        <section className="attendance-card attendance-error-list">
          <div className="attendance-table-heading">
            <div>
              <h3>取込エラー</h3>
              <p>{result.errors.length}件のエラーがあります。</p>
            </div>
          </div>

          <div className="attendance-table-wrapper">
            <table className="attendance-table">
              <thead>
                <tr>
                  <th>行番号</th>
                  <th>項目名</th>
                  <th>入力値</th>
                  <th>エラー内容</th>
                </tr>
              </thead>

              <tbody>
                {result.errors.map((error, index) => (
                  <tr
                    key={`${error.lineNumber}-${error.itemName}-${index}`}
                  >
                    <td>{error.lineNumber}</td>
                    <td>{error.itemName}</td>
                    <td>{error.value || '―'}</td>
                    <td className="attendance-error-message-cell">
                      {error.errorMessage}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`
}