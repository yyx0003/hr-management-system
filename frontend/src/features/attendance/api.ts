import axios from 'axios'
import { apiClient } from '../../lib/apiClient'
import { ATTENDANCE_API } from './constants'
import type {
  ApiErrorResponse,
  AttendanceCreateRequest,
  AttendanceCsvImportResponse,
  AttendanceListResponse,
  AttendanceUpdateRequest,
} from './types'

export async function getMonthlyAttendances(
  targetMonth: string,
): Promise<AttendanceListResponse> {
  const response = await apiClient.get<AttendanceListResponse>(
    ATTENDANCE_API.list,
    {
      params: { targetMonth },
    },
  )

  return response.data
}

export async function registerAttendance(
  request: AttendanceCreateRequest,
): Promise<void> {
  await apiClient.post(ATTENDANCE_API.register, request)
}

export async function updateAttendance(
  workDate: string,
  request: AttendanceUpdateRequest,
): Promise<void> {
  await apiClient.put(ATTENDANCE_API.update(workDate), request)
}

export async function importAttendanceCsv(
  targetMonth: string,
  file: File,
): Promise<AttendanceCsvImportResponse> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<AttendanceCsvImportResponse>(
    ATTENDANCE_API.csvImport,
    formData,
    {
      params: { targetMonth },
    },
  )

  return response.data
}

export async function exportHrCsv(targetMonth: string): Promise<void> {
  await downloadCsv(ATTENDANCE_API.hrCsvExport, targetMonth, '人事向け.csv')
}

export async function exportManagementCsv(
  targetMonth: string,
): Promise<void> {
  await downloadCsv(
    ATTENDANCE_API.managementCsvExport,
    targetMonth,
    '経営向け.csv',
  )
}

async function downloadCsv(
  url: string,
  targetMonth: string,
  defaultFileName: string,
): Promise<void> {
  const response = await apiClient.get<Blob>(url, {
    params: { targetMonth },
    responseType: 'blob',
  })

  const fileName =
    getFileNameFromContentDisposition(
      response.headers['content-disposition'],
    ) || `${targetMonth}_${defaultFileName}`

  const blobUrl = window.URL.createObjectURL(response.data)
  const anchor = document.createElement('a')

  anchor.href = blobUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()

  window.URL.revokeObjectURL(blobUrl)
}

function getFileNameFromContentDisposition(
  contentDisposition: string | undefined,
): string | null {
  if (!contentDisposition) return null

  const utf8Match = contentDisposition.match(
    /filename\*=UTF-8''([^;]+)/i,
  )

  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const normalMatch = contentDisposition.match(
    /filename="?([^"]+)"?/i,
  )

  return normalMatch?.[1] ?? null
}

export function getAttendanceApiErrorMessage(
  error: unknown,
  fallbackMessage: string,
): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const responseMessage = error.response?.data?.message

    if (responseMessage) {
      return responseMessage
    }

    if (!error.response) {
      return 'サーバーとの通信に失敗しました。'
    }
  }

  return fallbackMessage
}