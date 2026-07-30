import type {
  AttendanceEditRow,
  AttendanceListItem,
  WorkType,
} from './types'

export function toEditRow(
  item: AttendanceListItem,
): AttendanceEditRow {
  return {
    workDate: item.workDate,
    attendanceTime: normalizeTime(item.attendanceTime),
    leavingTime: normalizeTime(item.leavingTime),
    workType: item.workType ?? 'NORMAL',
    holidayType: item.holidayType ?? '',
    holidayName: item.holidayName ?? '',
    actualWorkHours: item.actualWorkHours,
    registered:
      item.attendanceTime !== null ||
      item.leavingTime !== null ||
      item.workType !== null,
  }
}

export function normalizeTime(value: string | null): string {
  if (!value) return ''

  return value.length >= 5 ? value.slice(0, 5) : value
}

export function toNullableTime(value: string): string | null {
  return value.trim() ? value : null
}

export function isTimeInputRequired(workType: WorkType): boolean {
  return workType === 'NORMAL' || workType === 'HOLIDAY_WORK'
}

export function getWorkTypeLabel(workType: WorkType): string {
  switch (workType) {
    case 'NORMAL':
      return '通常勤務'
    case 'PAID_LEAVE':
      return '有給休暇'
    case 'ABSENCE':
      return '欠勤'
    case 'HOLIDAY_WORK':
      return '休日出勤'
  }
}

export function getDayOfWeek(workDate: string): string {
  const day = new Date(`${workDate}T00:00:00`).getDay()

  return ['日', '月', '火', '水', '木', '金', '土'][day]
}

export function getCurrentMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')

  return `${year}-${month}`
}

export function changeMonth(
  targetMonth: string,
  amount: number,
): string {
  const [year, month] = targetMonth.split('-').map(Number)
  const date = new Date(year, month - 1 + amount, 1)

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    '0',
  )}`
}

export function formatDate(workDate: string): string {
  const [, month, day] = workDate.split('-')

  return `${Number(month)}/${Number(day)}`
}
