import {
  type ChangeEvent,
  type DragEvent,
  useRef,
  useState,
} from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../../constants/routes'
import { getAuthUser } from '../../auth/authStorage'
import {
  getAttendanceApiErrorMessage,
  importAttendanceCsv,
} from '../api'
import {
  CSV_MAX_FILE_SIZE,
  downloadAttendanceCsvTemplate,
  prepareAttendanceCsv,
  type CsvClientValidationError,
} from '../csvImportUtils'
import { ATTENDANCE_MESSAGES } from '../messages'
import type {
  AttendanceCsvImportResponse,
  CsvImportError,
} from '../types'
import { getCurrentMonth } from '../utils'
import '../attendance.css'

type DisplayError = {
  lineNumber?: number
  message: string
}

function getAttendanceDeadline(targetMonth: string): Date {
  const [year, month] = targetMonth.split('-').map(Number)
  return new Date(year, month, 5, 23, 59, 59, 999)
}

function isImportClosed(targetMonth: string): boolean {
  return new Date() > getAttendanceDeadline(targetMonth)
}

function formatTargetMonth(targetMonth: string): string {
  const [year, month] = targetMonth.split('-')
  return `${year}年${Number(month)}月`
}

function formatDeadline(targetMonth: string): string {
  const deadline = getAttendanceDeadline(targetMonth)
  return `${deadline.getFullYear()}年${deadline.getMonth() + 1}月5日`
}

export function AttendanceImportPage() {
  const navigate = useNavigate()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] =
    useState<AttendanceCsvImportResponse | null>(null)
  const [displayErrors, setDisplayErrors] = useState<DisplayError[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [importing, setImporting] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const closed = isImportClosed(targetMonth)
  const importSucceeded = Boolean(result?.success)
  const controlsDisabled = importing || closed || importSucceeded
  const importDisabled = controlsDisabled || !file
  const showUploadGuide = !file && !controlsDisabled

  const clearMessages = () => {
    setResult(null)
    setDisplayErrors([])
    setErrorMessage('')
  }

  const clearFile = () => {
    setFile(null)
    clearMessages()

    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  /* 別のCSVを取り込む場合に画面を初期状態へ戻す */
  const handleResetImport = () => {
    setFile(null)
    setResult(null)
    setDisplayErrors([])
    setErrorMessage('')
    setConfirmOpen(false)
    setDragging(false)

    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const selectFile = (selectedFile: File | null) => {
    clearMessages()

    if (!selectedFile) {
      setFile(null)
      return
    }

    if (!selectedFile.name.toLowerCase().endsWith('.csv')) {
      setFile(null)
      setErrorMessage(ATTENDANCE_MESSAGES.csvFileTypeInvalid)
      return
    }

    if (selectedFile.size > CSV_MAX_FILE_SIZE) {
      setFile(null)
      setErrorMessage(ATTENDANCE_MESSAGES.csvFileSizeExceeded)
      return
    }

    setFile(selectedFile)
  }

  const handleFileChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    selectFile(event.target.files?.[0] ?? null)
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setDragging(false)

    if (controlsDisabled) return

    if (event.dataTransfer.files.length !== 1) {
      setErrorMessage(ATTENDANCE_MESSAGES.csvSingleFileOnly)
      return
    }

    selectFile(event.dataTransfer.files[0] ?? null)
  }

  const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    if (!controlsDisabled) setDragging(true)
  }

  const handleDragLeave = (event: DragEvent<HTMLDivElement>) => {
    if (event.currentTarget.contains(event.relatedTarget as Node | null)) {
      return
    }
    setDragging(false)
  }

  const openFileDialog = () => {
    if (!controlsDisabled) fileInputRef.current?.click()
  }

  const handleImportRequest = () => {
    if (!file) {
      setErrorMessage(ATTENDANCE_MESSAGES.csvFileRequired)
      return
    }

    setConfirmOpen(true)
  }

  const handleImport = async () => {
    if (!file || importing || closed) return

    setConfirmOpen(false)
    setImporting(true)
    clearMessages()

    try {
      const authUser = getAuthUser()

      if (!authUser) {
        setErrorMessage('ログイン情報を確認できません。再度ログインしてください。')
        return
      }

      const prepared = await prepareAttendanceCsv(
        file,
        targetMonth,
        authUser.employeeNo,
      )

      if (!prepared.file) {
        setDisplayErrors(convertClientErrors(prepared.errors))
        setErrorMessage(ATTENDANCE_MESSAGES.csvImportValidationFailed)
        return
      }

      const response = await importAttendanceCsv(
        targetMonth,
        prepared.file,
      )

      setResult(response)

      if (!response.success) {
        setErrorMessage(
          response.message || ATTENDANCE_MESSAGES.csvImportValidationFailed,
        )
        setDisplayErrors(convertServerErrors(response.errors))
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
          <p className="attendance-page-eyebrow">CSV IMPORT</p>
          <h2>勤怠CSV取込</h2>
          <p className="attendance-page-description">
            CSVファイルから1か月分の勤怠情報を一括登録します。
          </p>
        </div>

        <button
          className="attendance-secondary-button attendance-back-button"
          type="button"
          disabled={importing}
          onClick={() => navigate(ROUTES.attendances)}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24">
            <path d="m15 18-6-6 6-6" />
          </svg>
          勤怠管理へ戻る
        </button>
      </div>

      <section className="attendance-card attendance-import-card">
        <div className="attendance-import-inner">
          <div className="attendance-import-warning">
            <span className="attendance-import-warning-icon" aria-hidden="true">
              !
            </span>
            <div>
              <strong>取り込み前にご確認ください</strong>
              <p>
                CSVを取り込むと、対象年月の登録済み勤怠情報は
                すべてCSVの内容で上書きされます。
              </p>
              <p className="attendance-import-deadline-note">
                勤怠CSVは対象月の翌月5日まで取り込めます。
              </p>
            </div>
          </div>

          {closed && (
            <div className="attendance-import-closed" role="status">
              <span className="attendance-import-closed-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="M12 8v4.25" />
                  <path d="M12 16.2h.01" />
                  <path d="M10.3 3.7 2.9 16.5a2 2 0 0 0 1.73 3h14.74a2 2 0 0 0 1.73-3L13.7 3.7a2 2 0 0 0-3.4 0Z" />
                </svg>
              </span>
              <div>
                <strong>入力締切済み</strong>
                <p>
                  {formatDeadline(targetMonth)}に勤怠入力を締め切りました。
                  CSVを取り込むことはできません。
                </p>
              </div>
            </div>
          )}

          <label className="attendance-form-field attendance-import-month-field">
            <span>対象年月</span>
            <input
              type="month"
              value={targetMonth}
              disabled={importing}
              onChange={(event) => {
                setTargetMonth(event.target.value)
                clearFile()
              }}
            />
          </label>

          <div className="attendance-form-field">
            <span>CSVファイル</span>

            <div
              aria-disabled={controlsDisabled}
              className={[
                'attendance-import-dropzone',
                showUploadGuide ? 'attendance-import-dropzone-guide' : '',
                dragging ? 'attendance-import-dropzone-dragging' : '',
                file ? 'attendance-import-dropzone-selected' : '',
                importSucceeded ? 'attendance-import-dropzone-success' : '',
                controlsDisabled ? 'attendance-import-dropzone-disabled' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              role="button"
              tabIndex={controlsDisabled ? -1 : 0}
              onClick={openFileDialog}
              onDragLeave={handleDragLeave}
              onDragOver={handleDragOver}
              onDrop={handleDrop}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  openFileDialog()
                }
              }}
            >
              <input
                ref={fileInputRef}
                accept=".csv,text/csv"
                className="attendance-import-file-input"
                type="file"
                disabled={controlsDisabled}
                onChange={handleFileChange}
              />

              {!file ? (
                <div className="attendance-import-dropzone-empty">
                  <span className="attendance-import-upload-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <path d="M12 16V4" />
                      <path d="m7.5 8.5 4.5-4.5 4.5 4.5" />
                      <path d="M5 14v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-4" />
                    </svg>
                  </span>

                  <strong>CSVファイルを選択</strong>
                  <span className="attendance-import-dropzone-description">
                    ファイルをドラッグ＆ドロップ、またはクリックして選択
                  </span>

                  <div className="attendance-import-dropzone-meta">
                    <span>対応形式：CSV</span>
                    <span>文字コード：UTF-8</span>
                    <span>最大ファイルサイズ：5MB</span>
                  </div>
                </div>
              ) : (
                <div className="attendance-import-selected-content">
                  <span className="attendance-import-file-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <path d="M7 3h7l4 4v14H7z" />
                      <path d="M14 3v5h5" />
                    </svg>
                  </span>
                  <div>
                    <strong>{file.name}</strong>
                    <span>{formatFileSize(file.size)}</span>
                  </div>
                  {!importSucceeded && (
                    <button
                      className="attendance-import-file-remove"
                      type="button"
                      disabled={importing || closed}
                      aria-label="選択したファイルを削除"
                      onClick={(event) => {
                        event.stopPropagation()
                        clearFile()
                      }}
                    >
                      <svg aria-hidden="true" viewBox="0 0 24 24">
                        <path d="M4 7h16" />
                        <path d="M9 7V4h6v3" />
                        <path d="M7 7l1 13h8l1-13" />
                        <path d="M10 11v5M14 11v5" />
                      </svg>
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="attendance-import-format-help">
              <p>
                項目：日付、勤務区分、出勤時間、退勤時間
              </p>
              <button
                className="attendance-template-download"
                type="button"
                disabled={importing}
                onClick={() => downloadAttendanceCsvTemplate(targetMonth)}
              >
                <svg aria-hidden="true" viewBox="0 0 24 24">
                  <path d="M12 3v12" />
                  <path d="m7.5 10.5 4.5 4.5 4.5-4.5" />
                  <path d="M5 20h14" />
                </svg>
                CSVテンプレートをダウンロード
              </button>
            </div>
          </div>

          {errorMessage && displayErrors.length === 0 && (
            <div className="attendance-alert attendance-alert-error" role="alert">
              {errorMessage}
            </div>
          )}

          {displayErrors.length > 0 && (
            <section className="attendance-import-error-panel" role="alert">
              <div className="attendance-import-error-heading">
                <span className="attendance-import-error-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24">
                    <path d="M12 8v4.5" />
                    <path d="M12 16.5h.01" />
                    <circle cx="12" cy="12" r="9" />
                  </svg>
                </span>
                <div>
                  <strong>CSVを取り込めませんでした</strong>
                  <p>
                    エラーがあるため、勤怠情報は登録されていません。
                  </p>
                </div>
              </div>

              <div className="attendance-import-error-items">
                {displayErrors.map((error, index) => (
                  <div
                    className="attendance-import-error-item"
                    key={`${error.lineNumber ?? 'general'}-${error.message}-${index}`}
                  >
                    {error.lineNumber && (
                      <span className="attendance-import-error-row">
                        {error.lineNumber}行目
                      </span>
                    )}
                    <span>{error.message}</span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {result?.success && (
            <div className="attendance-import-success" role="status">
              <span className="attendance-import-success-icon" aria-hidden="true">
                ✓
              </span>
              <div>
                <strong>
                  {result.message || ATTENDANCE_MESSAGES.csvImportSuccess}
                </strong>
                <p>{formatTargetMonth(targetMonth)}の勤怠情報を確認できます。</p>
              </div>
            </div>
          )}

          <div className="attendance-import-actions">
            {importSucceeded ? (
              <>
                <button
                  className="attendance-secondary-button"
                  type="button"
                  onClick={handleResetImport}
                >
                  別のCSVを取り込む
                </button>

                <button
                  className="attendance-primary-button"
                  type="button"
                  onClick={() => navigate(ROUTES.attendances)}
                >
                  勤怠管理画面で確認する
                </button>
              </>
            ) : (
              <button
                className="attendance-primary-button"
                disabled={importDisabled}
                type="button"
                onClick={handleImportRequest}
              >
                {importing ? (
                  <>
                    <span className="attendance-import-spinner" aria-hidden="true" />
                    取込中...
                  </>
                ) : (
                  'CSVを取り込む'
                )}
              </button>
            )}
          </div>
        </div>
      </section>

      {confirmOpen && (
        <div
          className="attendance-confirm-overlay"
          role="presentation"
          onMouseDown={(event) => {
            if (event.currentTarget === event.target) setConfirmOpen(false)
          }}
        >
          <section
            aria-labelledby="attendance-import-confirm-title"
            aria-modal="true"
            className="attendance-confirm-dialog"
            role="dialog"
          >
            <span className="attendance-confirm-icon" aria-hidden="true">!</span>
            <h3 id="attendance-import-confirm-title">勤怠CSVを取り込みます</h3>
            <p>
              {formatTargetMonth(targetMonth)}の登録済み勤怠情報は、
              CSVの内容ですべて上書きされます。
            </p>
            <p className="attendance-confirm-emphasis">
              この操作は取り消せません。取り込みを続けますか？
            </p>
            <div className="attendance-confirm-actions">
              <button
                className="attendance-secondary-button"
                type="button"
                onClick={() => setConfirmOpen(false)}
              >
                キャンセル
              </button>
              <button
                className="attendance-primary-button attendance-confirm-submit-button"
                type="button"
                onClick={() => void handleImport()}
              >
                取り込む
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

function convertClientErrors(
  errors: CsvClientValidationError[],
): DisplayError[] {
  return errors.map((error) => ({
    lineNumber: error.lineNumber,
    message: error.message,
  }))
}

function convertServerErrors(errors: CsvImportError[]): DisplayError[] {
  return errors.map((error) => ({
    lineNumber: error.lineNumber,
    message: error.errorMessage.replace(/\s*line=\d+\s*$/i, '').trim(),
  }))
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
