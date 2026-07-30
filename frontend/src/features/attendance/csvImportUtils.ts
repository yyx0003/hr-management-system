import type { WorkType } from './types'

const JAPANESE_HEADERS = [
  '日付',
  '勤務区分',
  '出勤時間',
  '退勤時間',
] as const

const ENGLISH_HEADERS = [
  'employeeNo',
  'workDate',
  'attendanceTime',
  'leavingTime',
  'workType',
] as const

const WORK_TYPE_MAP: Record<string, WorkType> = {
  通常勤務: 'NORMAL',
  有給休暇: 'PAID_LEAVE',
  欠勤: 'ABSENCE',
  休日出勤: 'HOLIDAY_WORK',
  NORMAL: 'NORMAL',
  PAID_LEAVE: 'PAID_LEAVE',
  ABSENCE: 'ABSENCE',
  HOLIDAY_WORK: 'HOLIDAY_WORK',
}

const UTF8_BOM = '\uFEFF'

export const CSV_MAX_FILE_SIZE = 5 * 1024 * 1024
export const CSV_TEMPLATE_FILE_NAME = '勤怠取込テンプレート.csv'

export interface CsvClientValidationError {
  lineNumber?: number
  message: string
}

export interface CsvPreparationResult {
  file: File | null
  errors: CsvClientValidationError[]
}

/**
 * 画面用の日本語CSVを、既存バックエンドが受け付ける形式へ変換する。
 * 社員番号はCSVに入力させず、ログイン中の社員番号を自動設定する。
 * 既存の英語ヘッダーCSVは引き続き利用できる。
 */
export async function prepareAttendanceCsv(
  sourceFile: File,
  targetMonth: string,
  loginEmployeeNo: string,
): Promise<CsvPreparationResult> {
  const text = removeBom(await sourceFile.text())
  const rows = parseCsv(text)

  if (rows.length === 0 || rows[0].every((value) => !value.trim())) {
    return {
      file: null,
      errors: [{ message: 'CSVファイルにデータがありません。' }],
    }
  }

  if (!loginEmployeeNo.trim()) {
    return {
      file: null,
      errors: [{ message: 'ログイン中の社員番号を確認できません。' }],
    }
  }

  const headers = rows[0].map((value) => value.trim())
  const isJapanese = headersMatch(headers, JAPANESE_HEADERS)
  const isEnglish = headersMatch(headers, ENGLISH_HEADERS)

  if (!isJapanese && !isEnglish) {
    return {
      file: null,
      errors: [
        {
          lineNumber: 1,
          message:
            'CSVのヘッダーが正しくありません。テンプレートを利用してください。',
        },
      ],
    }
  }

  const errors: CsvClientValidationError[] = []
  const convertedRows: string[][] = [[...ENGLISH_HEADERS]]

  rows.slice(1).forEach((row, index) => {
    const lineNumber = index + 2

    if (row.every((value) => !value.trim())) return

    const expectedLength = isJapanese ? 4 : 5

    if (row.length !== expectedLength) {
      errors.push({
        lineNumber,
        message: 'CSVの項目数が正しくありません。',
      })
      return
    }

    const normalized = row.map((value) => value.trim())

    const employeeNo = isJapanese
      ? loginEmployeeNo.trim()
      : normalized[0]
    const workDateSource = isJapanese ? normalized[0] : normalized[1]
    const workTypeSource = isJapanese ? normalized[1] : normalized[4]
    const attendanceTimeSource = isJapanese ? normalized[2] : normalized[2]
    const leavingTimeSource = isJapanese ? normalized[3] : normalized[3]

    const workDate = normalizeDate(workDateSource)
    const workType = WORK_TYPE_MAP[workTypeSource]
    const attendanceTime = normalizeOptionalTime(attendanceTimeSource)
    const leavingTime = normalizeOptionalTime(leavingTimeSource)

    if (!employeeNo) {
      errors.push({ lineNumber, message: '社員番号を確認できません。' })
    }

    if (!workDate) {
      errors.push({
        lineNumber,
        message:
          '日付はYYYY-MM-DDまたはYYYY/MM/DD形式で入力してください。',
      })
    } else if (!workDate.startsWith(`${targetMonth}-`)) {
      errors.push({
        lineNumber,
        message: `対象年月（${formatTargetMonth(targetMonth)}）以外の日付が含まれています。`,
      })
    }

    if (!workType) {
      errors.push({
        lineNumber,
        message:
          '勤務区分は「通常勤務」「有給休暇」「欠勤」「休日出勤」のいずれかを入力してください。',
      })
    }

    if (attendanceTime === null) {
      errors.push({
        lineNumber,
        message:
          '出勤時間はH:mm、HH:mm、H:mm:ssまたはHH:mm:ss形式で入力してください。',
      })
    }

    if (leavingTime === null) {
      errors.push({
        lineNumber,
        message:
          '退勤時間はH:mm、HH:mm、H:mm:ssまたはHH:mm:ss形式で入力してください。',
      })
    }

    if (
      employeeNo
      && workDate
      && workType
      && attendanceTime !== null
      && leavingTime !== null
    ) {
      convertedRows.push([
        employeeNo,
        workDate,
        attendanceTime,
        leavingTime,
        workType,
      ])
    }
  })

  if (convertedRows.length === 1 && errors.length === 0) {
    errors.push({ message: '取り込む勤怠データがありません。' })
  }

  if (errors.length > 0) {
    return { file: null, errors }
  }

  const csv = convertedRows
    .map((row) => row.map(escapeCsvValue).join(','))
    .join('\r\n')

  return {
    file: new File([csv], sourceFile.name, {
      type: 'text/csv;charset=utf-8',
    }),
    errors: [],
  }
}

/** 日本語ヘッダーのCSVテンプレートをダウンロードする。 */
export function downloadAttendanceCsvTemplate(targetMonth: string): void {
  const firstDate = `${targetMonth}-01`
  const secondDate = `${targetMonth}-02`
  const sample = [
    [...JAPANESE_HEADERS],
    [firstDate, '通常勤務', '09:00', '18:00'],
    [secondDate, '有給休暇', '', ''],
  ]

  const content = sample
    .map((row) => row.map(escapeCsvValue).join(','))
    .join('\r\n')

  const blob = new Blob([UTF8_BOM, content], {
    type: 'text/csv;charset=utf-8',
  })
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')

  anchor.href = url
  anchor.download = CSV_TEMPLATE_FILE_NAME
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}

function headersMatch(
  actual: string[],
  expected: readonly string[],
): boolean {
  return (
    actual.length === expected.length
    && expected.every((value, index) => actual[index] === value)
  )
}

function removeBom(value: string): string {
  return value.startsWith(UTF8_BOM) ? value.slice(1) : value
}

/**
 * Excelで「2026/7/1」のようにゼロ埋めが外れた日付も受け付け、
 * バックエンド用のYYYY-MM-DD形式に変換する。
 */
function normalizeDate(value: string): string | null {
  const match = value
    .trim()
    .match(/^(\d{4})[/-](\d{1,2})[/-](\d{1,2})$/)

  if (!match) return null

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(year, month - 1, day)

  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) {
    return null
  }

  return [
    String(year).padStart(4, '0'),
    String(month).padStart(2, '0'),
    String(day).padStart(2, '0'),
  ].join('-')
}

/**
 * Excelで「9:00:00」のように秒が付いた場合も受け付け、
 * バックエンド用のHH:mm形式に変換する。
 */
function normalizeOptionalTime(value: string): string | null {
  const trimmedValue = value.trim()

  if (!trimmedValue) return ''

  const match = trimmedValue.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/)

  if (!match) return null

  const hour = Number(match[1])
  const minute = Number(match[2])
  const second = match[3] === undefined ? 0 : Number(match[3])

  if (
    hour < 0
    || hour > 23
    || minute < 0
    || minute > 59
    || second < 0
    || second > 59
  ) {
    return null
  }

  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function formatTargetMonth(targetMonth: string): string {
  const [year, month] = targetMonth.split('-')
  return `${year}年${Number(month)}月`
}

function escapeCsvValue(value: string): string {
  if (!/[",\r\n]/.test(value)) return value
  return `"${value.replaceAll('"', '""')}"`
}

/** 引用符内のカンマと改行を考慮してCSVを読み取る。 */
function parseCsv(text: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let value = ''
  let quoted = false

  for (let index = 0; index < text.length; index += 1) {
    const character = text[index]
    const nextCharacter = text[index + 1]

    if (character === '"') {
      if (quoted && nextCharacter === '"') {
        value += '"'
        index += 1
      } else {
        quoted = !quoted
      }
      continue
    }

    if (character === ',' && !quoted) {
      row.push(value)
      value = ''
      continue
    }

    if ((character === '\n' || character === '\r') && !quoted) {
      if (character === '\r' && nextCharacter === '\n') index += 1
      row.push(value)
      rows.push(row)
      row = []
      value = ''
      continue
    }

    value += character
  }

  if (value || row.length > 0) {
    row.push(value)
    rows.push(row)
  }

  return rows
}