import {
    type ChangeEvent,
    useCallback,
    useEffect,
    useMemo,
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
import { WORK_TYPE_OPTIONS } from '../constants'
import { ATTENDANCE_MESSAGES } from '../messages'
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

const HOLIDAY_WORK_TYPE: WorkType = 'HOLIDAY_WORK'

type DeadlineStatus = 'normal' | 'warning' | 'closed'

function getAttendanceDeadline(targetMonth: string): Date {
    const [year, month] = targetMonth.split('-').map(Number)
    return new Date(year, month, 5, 23, 59, 59, 999)
}

function getDeadlineStatus(targetMonth: string): DeadlineStatus {
    const now = new Date()
    const deadline = getAttendanceDeadline(targetMonth)

    if (now > deadline) {
        return 'closed'
    }

    const millisecondsPerDay = 24 * 60 * 60 * 1000
    const remainingDays = Math.ceil(
        (deadline.getTime() - now.getTime()) / millisecondsPerDay,
    )

    return remainingDays <= 7 ? 'warning' : 'normal'
}

function isWeekend(workDate: string): boolean {
    const date = new Date(`${workDate}T00:00:00`)
    const day = date.getDay()

    return day === 0 || day === 6
}

function formatDeadline(targetMonth: string): string {
    const [year, month] = targetMonth.split('-').map(Number)
    const deadline = new Date(year, month, 5)

    return `${deadline.getFullYear()}年${deadline.getMonth() + 1}月5日`
}

function formatTargetMonth(targetMonth: string): string {
    const [year, month] = targetMonth.split('-')
    return `${year}年${Number(month)}月`
}

function rowsAreEqual(
    currentRow: AttendanceEditRow,
    originalRow: AttendanceEditRow | undefined,
): boolean {
    if (!originalRow) return false

    return (
        currentRow.workType === originalRow.workType
        && currentRow.attendanceTime === originalRow.attendanceTime
        && currentRow.leavingTime === originalRow.leavingTime
    )
}

function hasRequiredInput(row: AttendanceEditRow): boolean {
    if (isTimeInputRequired(row.workType)) {
        return Boolean(row.attendanceTime && row.leavingTime)
    }

    return true
}

function DeadlineIcon({ status }: { status: DeadlineStatus }) {
    if (status === 'closed') {
        return (
            <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M12 8v4.25" />
                <path d="M12 16.2h.01" />
                <path d="M10.3 3.7 2.9 16.5a2 2 0 0 0 1.73 3h14.74a2 2 0 0 0 1.73-3L13.7 3.7a2 2 0 0 0-3.4 0Z" />
            </svg>
        )
    }

    if (status === 'warning') {
        return (
            <span
                aria-hidden="true"
                className="attendance-deadline-fire"
            >
                🔥
            </span>
        )
    }
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
            <path d="M7 3v3M17 3v3M4 9h16" />
            <rect x="4" y="5" width="16" height="15" rx="2" />
            <path d="M8 13h.01M12 13h.01M16 13h.01M8 17h.01M12 17h.01" />
        </svg>
    )
}

function SummaryCalendarIcon() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
            <path d="M7 3v3M17 3v3M4 9h16" />
            <rect x="4" y="5" width="16" height="15" rx="2" />
            <path d="M8 13h2M12 13h2M16 13h.01M8 17h2M12 17h2" />
        </svg>
    )
}

function SummaryCheckIcon() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="8.5" />
            <path d="m8.5 12.2 2.2 2.2 4.8-5" />
        </svg>
    )
}

function SummaryClockIcon() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="8.5" />
            <path d="M12 7.5v5l3 1.8" />
        </svg>
    )
}

export function AttendancePage() {
    const navigate = useNavigate()

    const [targetMonth, setTargetMonth] = useState(getCurrentMonth())
    const [rows, setRows] = useState<AttendanceEditRow[]>([])
    const [originalRows, setOriginalRows] = useState<
        Record<string, AttendanceEditRow>
    >({})
    const [loading, setLoading] = useState(false)
    const [savingDate, setSavingDate] = useState<string | null>(null)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    const deadlineStatus = useMemo(
        () => getDeadlineStatus(targetMonth),
        [targetMonth],
    )

    const isClosed = deadlineStatus === 'closed'

    const isHolidayRow = useCallback(
        (row: AttendanceEditRow): boolean =>
            isWeekend(row.workDate) || Boolean(row.holidayName),
        [],
    )

    const registeredWorkdayCount = useMemo(
        () =>
            rows.filter((row) => {
                if (!row.registered) return false

                const holiday = isHolidayRow(row)
                return !holiday || row.workType === HOLIDAY_WORK_TYPE
            }).length,
        [rows, isHolidayRow],
    )

    const unregisteredWorkdayCount = useMemo(
        () =>
            rows.filter(
                (row) =>
                    !row.registered
                    && !isHolidayRow(row),
            ).length,
        [rows, isHolidayRow],
    )

    const scheduledWorkdayCount = useMemo(
        () =>
            rows.filter((row) => !isHolidayRow(row)).length,
        [rows, isHolidayRow],
    )

    const loadAttendances = useCallback(async () => {
        setLoading(true)
        setMessage('')
        setErrorMessage('')

        try {
            const response = await getMonthlyAttendances(targetMonth)
            const editRows = response.attendanceList.map(toEditRow)

            setRows(editRows)
            setOriginalRows(
                Object.fromEntries(
                    editRows.map((row) => [
                        row.workDate,
                        { ...row },
                    ]),
                ),
            )
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

    useEffect(() => {
        if (!message) return undefined

        const timerId = window.setTimeout(() => {
            setMessage('')
        }, 3000)

        return () => {
            window.clearTimeout(timerId)
        }
    }, [message])

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
                        attendanceTime: requiresTime
                            ? row.attendanceTime
                            : '',
                        leavingTime: requiresTime
                            ? row.leavingTime
                            : '',
                    }
                }

                return {
                    ...row,
                    [field]: value,
                }
            }),
        )
    }

    const validateRow = (
        row: AttendanceEditRow,
    ): string | null => {
        const requiresTime = isTimeInputRequired(row.workType)

        if (
            requiresTime
            && (!row.attendanceTime || !row.leavingTime)
        ) {
            return ATTENDANCE_MESSAGES.timeRequired
        }

        if (
            !requiresTime
            && (row.attendanceTime || row.leavingTime)
        ) {
            return ATTENDANCE_MESSAGES.timeNotAllowed
        }

        if (
            row.attendanceTime
            && row.leavingTime
            && row.attendanceTime >= row.leavingTime
        ) {
            return ATTENDANCE_MESSAGES.startAfterEnd
        }

        return null
    }

    const handleSave = async (row: AttendanceEditRow) => {
        if (isClosed) {
            setMessage('')
            setErrorMessage(
                `この年月の勤怠入力は${formatDeadline(targetMonth)}に締め切られました。`,
            )
            return
        }

        const holiday = isHolidayRow(row)

        if (holiday && row.workType !== HOLIDAY_WORK_TYPE) {
            setMessage('')
            setErrorMessage(
                `${formatDate(row.workDate)}は休日です。登録する場合は「休日出勤」を選択してください。`,
            )
            return
        }

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
                        月ごとの出退勤時間と勤務区分を登録・更新します。
                    </p>
                </div>

                <div className="attendance-header-actions">
                    <button
                        className="attendance-secondary-button"
                        type="button"
                        disabled={isClosed}
                        onClick={() => navigate(ROUTES.attendanceImport)}
                    >
                        <svg
                            aria-hidden="true"
                            className="attendance-secondary-button-icon"
                            viewBox="0 0 24 24"
                        >
                            <path d="M12 16V4" />
                            <path d="m7.5 8.5 4.5-4.5 4.5 4.5" />
                            <path d="M5 14v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-4" />
                        </svg>

                        <span>CSV取込</span>
                    </button>


                </div>
            </div>

            <section className="attendance-card attendance-filter-card">
                <div className="attendance-filter-controls">
                    <button
                        aria-label="前月"
                        className="attendance-month-button"
                        type="button"
                        onClick={() =>
                            setTargetMonth((current) =>
                                changeMonth(current, -1)
                            )
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
                            setTargetMonth((current) =>
                                changeMonth(current, 1)
                            )
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
                        ↻ 再読込
                    </button>
                </div>

                <div className="attendance-summary-grid">
                    <article className="attendance-summary-card attendance-summary-calendar">
                        <span className="attendance-summary-icon">
                            <SummaryCalendarIcon />
                        </span>

                        <div>
                            <span className="attendance-summary-label">
                                所定勤務日
                            </span>

                            <strong className="attendance-summary-value">
                                {scheduledWorkdayCount}
                                <small>日</small>
                            </strong>
                        </div>
                    </article>

                    <article className="attendance-summary-card attendance-summary-registered">
                        <span className="attendance-summary-icon">
                            <SummaryCheckIcon />
                        </span>

                        <div>
                            <span className="attendance-summary-label">
                                登録済
                            </span>

                            <strong className="attendance-summary-value">
                                {registeredWorkdayCount}
                                <small>日</small>
                            </strong>
                        </div>
                    </article>

                    <article className="attendance-summary-card attendance-summary-unregistered">
                        <span className="attendance-summary-icon">
                            <SummaryClockIcon />
                        </span>

                        <div>
                            <span className="attendance-summary-label">
                                未登録
                            </span>

                            <strong className="attendance-summary-value">
                                {unregisteredWorkdayCount}
                                <small>日</small>
                            </strong>
                        </div>
                    </article>
                </div>
            </section>

            <div
                className={`attendance-deadline attendance-deadline-${deadlineStatus}`}
            >
                <span className="attendance-deadline-icon">
                    <DeadlineIcon status={deadlineStatus} />
                </span>

                <div className="attendance-deadline-content">
                    <strong>
                        {deadlineStatus === 'closed'
                            ? '入力締切済み'
                            : '入力期限'}
                    </strong>

                    <span>
                        {deadlineStatus === 'closed'
                            ? `この年月の勤怠入力は${formatDeadline(targetMonth)}に締め切られました。`
                            : formatDeadline(targetMonth)}
                    </span>
                </div>
            </div>

            {message && (
                <div
                    className="attendance-toast attendance-toast-success"
                    role="status"
                >
                    <span className="attendance-toast-icon">✓</span>
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
                        <h3>{formatTargetMonth(targetMonth)}の勤怠</h3>

                        <p className="attendance-table-summary">
                            {rows.length}日間・所定勤務日
                            {scheduledWorkdayCount}日
                        </p>

                        <p className="attendance-table-note">
                            <span aria-hidden="true">※</span>
                            通常勤務・休日出勤は、出勤時間と退勤時間の入力が必要です。
                        </p>
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
                                <th>出勤時間</th>
                                <th>退勤時間</th>
                                <th>状態</th>
                                <th>操作</th>
                            </tr>
                        </thead>

                        <tbody>
                            {loading ? (
                                <tr>
                                    <td
                                        className="attendance-empty-cell"
                                        colSpan={8}
                                    >
                                        読み込み中です...
                                    </td>
                                </tr>
                            ) : rows.length === 0 ? (
                                <tr>
                                    <td
                                        className="attendance-empty-cell"
                                        colSpan={8}
                                    >
                                        表示する勤怠情報がありません。
                                    </td>
                                </tr>
                            ) : (
                                rows.map((row) => {
                                    const weekend = isWeekend(row.workDate)
                                    const holiday = isHolidayRow(row)
                                    const holidayWorkSelected =
                                        row.workType === HOLIDAY_WORK_TYPE

                                    const timeRequired =
                                        isTimeInputRequired(row.workType)

                                    const timeInputDisabled =
                                        isClosed
                                        || !timeRequired
                                        || (holiday && !holidayWorkSelected)

                                    const originalRow = originalRows[row.workDate]
                                    const dirty = !rowsAreEqual(row, originalRow)
                                    const inputComplete = hasRequiredInput(row)

                                    const isSaving =
                                        savingDate === row.workDate

                                    const targetExcluded =
                                        holiday && !holidayWorkSelected

                                    const saveDisabled =
                                        isClosed
                                        || isSaving
                                        || targetExcluded
                                        || !dirty
                                        || !inputComplete

                                    const dayOfWeek =
                                        getDayOfWeek(row.workDate)

                                    const statusLabel = targetExcluded
                                        ? '対象外'
                                        : dirty
                                            ? '入力中'
                                            : row.registered
                                                ? '登録済'
                                                : '未登録'

                                    const statusClass = targetExcluded
                                        ? 'attendance-status-target'
                                        : dirty
                                            ? 'attendance-status-editing'
                                            : row.registered
                                                ? 'attendance-status-registered'
                                                : 'attendance-status-unregistered'

                                    const actionLabel = row.registered
                                        ? '更新'
                                        : '登録'

                                    const selectClassName = targetExcluded
                                        ? 'attendance-work-type-holiday'
                                        : row.workType === 'PAID_LEAVE'
                                            ? 'attendance-work-type-paid-leave'
                                            : row.workType === 'ABSENCE'
                                                ? 'attendance-work-type-absence'
                                                : row.workType === HOLIDAY_WORK_TYPE
                                                    ? 'attendance-work-type-holiday-work'
                                                    : 'attendance-work-type-normal'

                                    const rowClassName = dayOfWeek === '土'
                                        ? 'attendance-saturday-row'
                                        : dayOfWeek === '日' || Boolean(row.holidayName)
                                            ? 'attendance-holiday-row'
                                            : ''

                                    return (
                                        <tr
                                            className={rowClassName}
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
                                                    <span
                                                        className={
                                                            dayOfWeek === '土'
                                                                ? 'attendance-holiday-label attendance-holiday-label-saturday'
                                                                : 'attendance-holiday-label'
                                                        }
                                                    >
                                                        {row.holidayName}
                                                    </span>
                                                ) : weekend ? (
                                                    <span
                                                        className={
                                                            dayOfWeek === '土'
                                                                ? 'attendance-weekend-label attendance-weekend-label-saturday'
                                                                : 'attendance-weekend-label attendance-weekend-label-sunday'
                                                        }
                                                    >
                                                        {dayOfWeek === '土' ? '土曜日' : '日曜日'}
                                                    </span>
                                                ) : (
                                                    <span className="attendance-muted">
                                                        —
                                                    </span>
                                                )}
                                            </td>

                                            <td>
                                                <div
                                                    className={`attendance-work-type-control ${selectClassName}`}
                                                >
                                                    <select
                                                        aria-label={`${row.workDate}の勤務区分`}
                                                        value={
                                                            holiday && !holidayWorkSelected
                                                                ? '__HOLIDAY__'
                                                                : row.workType
                                                        }
                                                        disabled={isClosed}
                                                        onChange={(event) => {
                                                            const value = event.target.value

                                                            updateRow(
                                                                row.workDate,
                                                                'workType',
                                                                value === '__HOLIDAY__'
                                                                    ? 'NORMAL'
                                                                    : value,
                                                            )
                                                        }}
                                                    >
                                                        {holiday ? (
                                                            <>
                                                                <option value="__HOLIDAY__">
                                                                    休日
                                                                </option>
                                                                <option value={HOLIDAY_WORK_TYPE}>
                                                                    休日出勤
                                                                </option>
                                                            </>
                                                        ) : (
                                                            WORK_TYPE_OPTIONS.map((option) => (
                                                                <option
                                                                    key={option.value}
                                                                    value={option.value}
                                                                >
                                                                    {option.label}
                                                                </option>
                                                            ))
                                                        )}
                                                    </select>
                                                </div>
                                            </td>

                                            <td>
                                                <input
                                                    aria-label={`${row.workDate}の出勤時間`}
                                                    disabled={timeInputDisabled}
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
                                                    aria-label={`${row.workDate}の退勤時間`}
                                                    disabled={timeInputDisabled}
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
                                                    className={`attendance-status ${statusClass}`}
                                                >
                                                    {statusLabel}
                                                </span>
                                            </td>

                                            <td>
                                                {targetExcluded ? (
                                                    <span className="attendance-action-empty">
                                                        —
                                                    </span>
                                                ) : (
                                                    <button
                                                        className={
                                                            dirty && inputComplete
                                                                ? 'attendance-row-save-button attendance-row-save-button-active'
                                                                : 'attendance-row-save-button'
                                                        }
                                                        disabled={saveDisabled}
                                                        type="button"
                                                        onClick={() =>
                                                            void handleSave(row)
                                                        }
                                                    >
                                                        {isSaving
                                                            ? '保存中'
                                                            : actionLabel}
                                                    </button>
                                                )}
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