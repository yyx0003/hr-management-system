import axios from 'axios'
import { apiClient } from '../../lib/apiClient'
import { ATTENDANCE_API } from './constants'
import type {
  ApiErrorResponse,
  AttendanceCreateRequest,
  AttendanceCsvImportResponse,
  AttendanceListResponse,
  AttendanceMonthlyDeleteResponse,
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

export async function deleteMonthlyAttendances(
  targetMonth: string,
): Promise<AttendanceMonthlyDeleteResponse> {
  const response =
    await apiClient.delete<AttendanceMonthlyDeleteResponse>(
      ATTENDANCE_API.list,
      {
        params: { targetMonth },
      },
    )

  return response.data
}

export function getAttendanceDeleteApiErrorMessage(
  error: unknown,
  fallbackMessage: string,
): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const responseMessage = error.response?.data?.message
    if (responseMessage) return responseMessage
  }

  return fallbackMessage
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

export async function exportHrCsv(
  targetMonth: string,
): Promise<DownloadedCsv> {
  return requestCsvExport(
    ATTENDANCE_API.hrCsvExport,
    targetMonth,
    `人事向け_勤怠給与_${targetMonth.replace('-', '')}.csv`,
  )
}

export async function exportManagementCsv(
  targetMonth: string,
): Promise<DownloadedCsv> {
  return requestCsvExport(
    ATTENDANCE_API.managementCsvExport,
    targetMonth,
    `経営向け_部署別集計_${targetMonth.replace('-', '')}.csv`,
  )
}

export interface DownloadedCsv {
  blob: Blob
  fileName: string
}

async function requestCsvExport(
  url: string,
  targetMonth: string,
  fallbackFileName: string,
): Promise<DownloadedCsv> {
  const response = await apiClient.post<Blob>(url, { targetMonth }, {
    responseType: 'blob',
  })

  const fileName =
    getFileNameFromContentDisposition(
      response.headers['content-disposition'],
    ) || fallbackFileName

  return {
    blob: response.data,
    fileName,
  }
}

function getFileNameFromContentDisposition(
  contentDisposition: string | undefined,
): string | null {
  if (!contentDisposition) return null

  const utf8Match = contentDisposition.match(
    /filename\*=UTF-8''([^;]+)/i,
  )

  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      // 不正なfilename*の場合は下の通常形式、または動的な予備名を使用する。
    }
  }

  const normalMatch = contentDisposition.match(
    /filename="?([^"]+)"?/i,
  )

  return normalMatch?.[1] ?? null
}

export async function getCsvExportApiErrorMessage(
  error: unknown,
): Promise<string | null> {
  if (!axios.isAxiosError<ApiErrorResponse | Blob>(error)) {
    return null
  }

  const responseData = error.response?.data

  if (responseData instanceof Blob) {
    return getMessageFromErrorBlob(responseData)
  }

  if (
    responseData &&
    typeof responseData === 'object' &&
    'message' in responseData &&
    typeof responseData.message === 'string'
  ) {
    return responseData.message
  }

  return null
}

async function getMessageFromErrorBlob(
  errorBlob: Blob,
): Promise<string | null> {
  let text: string
  try {
    text = await errorBlob.text()
  } catch {
    return null
  }

  if (!text) return null

  try {
    const response: unknown = JSON.parse(text)
    if (
      response &&
      typeof response === 'object' &&
      'message' in response &&
      typeof response.message === 'string'
    ) {
      return response.message
    }
  } catch {
    // JSON以外のエラーレスポンスは共通の予備メッセージへ委ねる。
  }

  return null
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
