import type { WorkType } from './types'

export const ATTENDANCE_API = {
  list: '/api/attendances',
  register: '/api/attendances',
  update: (workDate: string) =>
    `/api/attendances/${encodeURIComponent(workDate)}`,
  csvImport: '/api/attendances/csv-import',
  hrCsvExport: '/api/csv/hr/export',
  managementCsvExport: '/api/csv/management/export',
} as const

export const WORK_TYPE_OPTIONS: Array<{
  value: WorkType
  label: string
}> = [
  { value: 'NORMAL', label: '通常勤務' },
  { value: 'PAID_LEAVE', label: '有給休暇' },
  { value: 'ABSENCE', label: '欠勤' },
  { value: 'HOLIDAY_WORK', label: '休日出勤' },
]

export const CSV_EXPORT_TYPE = {
  hr: 'HR',
  management: 'MANAGEMENT',
} as const

export type CsvExportType =
  (typeof CSV_EXPORT_TYPE)[keyof typeof CSV_EXPORT_TYPE]
