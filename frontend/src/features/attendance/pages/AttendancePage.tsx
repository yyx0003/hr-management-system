import {
  type ChangeEvent,
  useCallback,
  useEffect,
  useState,
} from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../../constants/routes'
import {
  getAttendanceApiErrorMessage,
  getMonthlyAttendances,
  registerAttendance,
  updateAttendance,
} from '../api'
import {
  ATTENDANCE_MESSAGES,
} from '../messages'
import {
  WORK_TYPE_OPTIONS,
} from '../constants'
import type {
  AttendanceEditRow,
  WorkType,
} from '../types'
import {
  changeMonth,
  formatDate,
  getCurrentMonth,
  getDayOfWeek,
  isTimeInputRequired,
  toEditRow,
  toNullableTime,
} from '../utils'
import '../attendance.css'

export function AttendancePage() {
  const navigate = useNavigate()
  const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
  const [rows, setRows] = useState<AttendanceEditRow[]>([])
  const [loading, setLoading] = useState(false)
  const [savingDate, setSavingDate] = useState<string | null>(null)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const loadAttendances = useCallback(async () => {
    setLoading(true)
    setMessage('')
    setErrorMessage('')

    try {
      const response = await getMonthlyAttendances(targetMonth)
      setRows(response.attendanceList.map(toEditRow))
    } catch (error: unknown) {
      setErrorMessage(
        getAttendanceApiErrorMessage(
          error,
          ATTENDANCE_MESSAGES.loadFailed,
        ),
      )
    } finally {
      setLoading(false)
    }
  }, [targetMonth])

 useEffect(() => {
  const timerId = window.setTimeout(() => {
    void loadAttendances()
  }, 0)

  return () => {
    window.clearTimeout(timerId)
  }
}, [loadAttendances])

  const updateRow = (
    workDate: string,
    field: 'attendanceTime' | 'leavingTime' | 'workType',
    value: string,
  ) => {
    setRows((currentRows) =>
      currentRows.map((row) => {
        if (row.workDate !== workDate) return row

        if (field === 'workType') {
          const nextWorkType = value as WorkType
          const requiresTime = isTimeInputRequired(nextWorkType)

          return {
            ...row,
            workType: nextWorkType,
            attendanceTime: requiresTime ? row.attendanceTime : '',
            leavingTime: requiresTime ? row.leavingTime : '',
          }
        }

        return {
          ...row,
          [field]: value,
        }
      }),
    )
  }

  const validateRow = (row: AttendanceEditRow): string | null => {
    const requiresTime = isTimeInputRequired(row.workType)

    if (
      requiresTime &&
      (!row.attendanceTime || !row.leavingTime)
    ) {
      return ATTENDANCE_MESSAGES.timeRequired
    }

    if (
      !requiresTime &&
      (row.attendanceTime || row.leavingTime)
    ) {
      return ATTENDANCE_MESSAGES.timeNotAllowed
    }

    if (
      row.attendanceTime &&
      row.leavingTime &&
      row.attendanceTime >= row.leavingTime
    ) {
      return ATTENDANCE_MESSAGES.startAfterEnd
    }

    return null
  }

  const handleSave = async (row: AttendanceEditRow) => {
    const validationMessage = validateRow(row)

    if (validationMessage) {
      setMessage('')
      setErrorMessage(
        `${formatDate(row.workDate)}：${validationMessage}`,
      )
      return
    }

    setSavingDate(row.workDate)
    setMessage('')
    setErrorMessage('')

    try {
      const request = {
        attendanceTime: toNullableTime(row.attendanceTime),
        leavingTime: toNullableTime(row.leavingTime),
        workType: row.workType,
      }

      if (row.registered) {
        await updateAttendance(row.workDate, request)
        setMessage(ATTENDANCE_MESSAGES.updateSuccess)
      } else {
        await registerAttendance({
          workDate: row.workDate,
          ...request,
        })
        setMessage(ATTENDANCE_MESSAGES.registerSuccess)
      }

      await loadAttendances()
    } catch (error: unknown) {
      setErrorMessage(
        getAttendanceApiErrorMessage(
          error,
          row.registered
            ? ATTENDANCE_MESSAGES.updateFailed
            : ATTENDANCE_MESSAGES.registerFailed,
        ),
      )
    } finally {
      setSavingDate(null)
    }
  }

  const handleMonthChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    setTargetMonth(event.target.value)
  }

  return (
    <div className="attendance-page">
      <div className="attendance-page-header">
        <div>
          <p className="attendance-page-eyebrow">
            ATTENDANCE MANAGEMENT
          </p>
          <h2>勤怠管理</h2>
          <p className="attendance-page-description">
            月ごとの出退勤時刻と勤務区分を登録・更新します。
          </p>
        </div>

        <div className="attendance-header-actions">
          <button
            className="attendance-secondary-button"
            type="button"
            onClick={() => navigate(ROUTES.attendanceImport)}
          >
            CSV取込
          </button>

          <button
            className="attendance-primary-button"
            type="button"
            onClick={() => navigate(ROUTES.csvExport)}
          >
            CSV出力
          </button>
        </div>
      </div>

      <section className="attendance-card attendance-filter-card">
        <button
          aria-label="前月"
          className="attendance-month-button"
          type="button"
          onClick={() =>
            setTargetMonth((current) => changeMonth(current, -1))
          }
        >
          ‹
        </button>

        <label className="attendance-month-field">
          <span>対象年月</span>
          <input
            type="month"
            value={targetMonth}
            onChange={handleMonthChange}
          />
        </label>

        <button
          aria-label="翌月"
          className="attendance-month-button"
          type="button"
          onClick={() =>
            setTargetMonth((current) => changeMonth(current, 1))
          }
        >
          ›
        </button>

        <button
          className="attendance-secondary-button"
          type="button"
          disabled={loading}
          onClick={() => void loadAttendances()}
        >
          再読込
        </button>
      </section>

      {message && (
        <div className="attendance-alert attendance-alert-success">
          {message}
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

      <section className="attendance-card attendance-table-card">
        <div className="attendance-table-heading">
          <div>
            <h3>{targetMonth.replace('-', '年')}月の勤怠</h3>
            <p>{rows.length}日分の勤務情報</p>
          </div>
        </div>

        <div className="attendance-table-wrapper">
          <table className="attendance-table">
            <thead>
              <tr>
                <th>日付</th>
                <th>曜日</th>
                <th>休日</th>
                <th>勤務区分</th>
                <th>出勤時刻</th>
                <th>退勤時刻</th>
                <th>状態</th>
                <th>操作</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td className="attendance-empty-cell" colSpan={8}>
                    読み込み中です...
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td className="attendance-empty-cell" colSpan={8}>
                    表示する勤怠情報がありません。
                  </td>
                </tr>
              ) : (
                rows.map((row) => {
                  const timeRequired = isTimeInputRequired(
                    row.workType,
                  )
                  const isSaving = savingDate === row.workDate
                  const dayOfWeek = getDayOfWeek(row.workDate)
                  const weekend =
                    dayOfWeek === '土' || dayOfWeek === '日'

                  return (
                    <tr
                      className={
                        weekend || row.holidayName
                          ? 'attendance-holiday-row'
                          : ''
                      }
                      key={row.workDate}
                    >
                      <td className="attendance-date-cell">
                        {formatDate(row.workDate)}
                      </td>

                      <td>
                        <span
                          className={`attendance-day attendance-day-${dayOfWeek}`}
                        >
                          {dayOfWeek}
                        </span>
                      </td>

                      <td>
                        {row.holidayName ? (
                          <span className="attendance-holiday-label">
                            {row.holidayName}
                          </span>
                        ) : (
                          <span className="attendance-muted">―</span>
                        )}
                      </td>

                      <td>
                        <select
                          aria-label={`${row.workDate}の勤務区分`}
                          value={row.workType}
                          onChange={(event) =>
                            updateRow(
                              row.workDate,
                              'workType',
                              event.target.value,
                            )
                          }
                        >
                          {WORK_TYPE_OPTIONS.map((option) => (
                            <option
                              key={option.value}
                              value={option.value}
                            >
                              {option.label}
                            </option>
                          ))}
                        </select>
                      </td>

                      <td>
                        <input
                          aria-label={`${row.workDate}の出勤時刻`}
                          disabled={!timeRequired}
                          type="time"
                          value={row.attendanceTime}
                          onChange={(event) =>
                            updateRow(
                              row.workDate,
                              'attendanceTime',
                              event.target.value,
                            )
                          }
                        />
                      </td>

                      <td>
                        <input
                          aria-label={`${row.workDate}の退勤時刻`}
                          disabled={!timeRequired}
                          type="time"
                          value={row.leavingTime}
                          onChange={(event) =>
                            updateRow(
                              row.workDate,
                              'leavingTime',
                              event.target.value,
                            )
                          }
                        />
                      </td>

                      <td>
                        <span
                          className={
                            row.registered
                              ? 'attendance-status attendance-status-registered'
                              : 'attendance-status attendance-status-unregistered'
                          }
                        >
                          {row.registered ? '登録済' : '未登録'}
                        </span>
                      </td>

                      <td>
                        <button
                          className="attendance-row-save-button"
                          disabled={isSaving}
                          type="button"
                          onClick={() => void handleSave(row)}
                        >
                          {isSaving
                            ? '保存中'
                            : row.registered
                              ? '更新'
                              : '登録'}
                        </button>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}