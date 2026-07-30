export type WorkType =
  | 'NORMAL'
  | 'PAID_LEAVE'
  | 'ABSENCE'
  | 'HOLIDAY_WORK'

export interface AttendanceListItem {
  workDate: string
  attendanceTime: string | null
  leavingTime: string | null
  workType: WorkType | null
  holidayType: string | null
  holidayName: string | null
}

export interface AttendanceListResponse {
  targetMonth: string
  attendanceList: AttendanceListItem[]
}

export interface AttendanceCreateRequest {
  workDate: string
  attendanceTime: string | null
  leavingTime: string | null
  workType: WorkType
}

export interface AttendanceUpdateRequest {
  attendanceTime: string | null
  leavingTime: string | null
  workType: WorkType
}

export interface CsvImportError {
  lineNumber: number
  itemName: string
  value: string
  errorMessage: string
}

export interface AttendanceCsvImportResponse {
  success: boolean
  message: string
  errors: CsvImportError[]
}

export interface ApiErrorResponse {
  message?: string
}

export interface AttendanceEditRow {
  workDate: string
  attendanceTime: string
  leavingTime: string
  workType: WorkType
  holidayType: string
  holidayName: string
  registered: boolean
}