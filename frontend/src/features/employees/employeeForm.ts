import type { EmployeeDetail, EmployeeFormRequest, EmployeeFormValues } from './types'

export type EmployeeFormErrors = Partial<Record<Exclude<keyof EmployeeFormValues, 'qualifications'>, string>> & {
  qualifications: Array<{
    qualificationId: string
    acquisitionDate: string
  }>
}

export const EMPTY_EMPLOYEE_FORM_VALUES: EmployeeFormValues = {
  employeeName: '',
  birthDate: '',
  postalCode: '',
  address: '',
  phoneNumber: '',
  emailAddress: '',
  hireDate: '',
  retireDate: '',
  departmentId: '',
  positionId: '',
  skillGrade: '',
  qualifications: [],
}

export function toEmployeeFormValues(employee: EmployeeDetail): EmployeeFormValues {
  return {
    employeeName: employee.employeeName,
    birthDate: employee.birthDate,
    postalCode: employee.postalCode,
    address: employee.address,
    phoneNumber: employee.phoneNumber ?? '',
    emailAddress: employee.emailAddress ?? '',
    hireDate: employee.hireDate,
    retireDate: employee.retireDate ?? '',
    departmentId: String(employee.departmentId),
    positionId: employee.positionId === null ? '' : String(employee.positionId),
    skillGrade: String(employee.skillGrade),
    qualifications: employee.qualifications.map((qualification) => ({
      qualificationId: String(qualification.qualificationId),
      acquisitionDate: qualification.acquisitionDate,
    })),
  }
}

export function validateEmployeeForm(values: EmployeeFormValues, requiresHireDate: boolean): EmployeeFormErrors {
  const errors: EmployeeFormErrors = { qualifications: values.qualifications.map(() => ({ qualificationId: '', acquisitionDate: '' })) }
  const employeeName = values.employeeName.trim()
  const postalCode = values.postalCode.trim()
  const address = values.address.trim()
  const phoneNumber = values.phoneNumber.trim()
  const emailAddress = values.emailAddress.trim()

  if (!employeeName) errors.employeeName = '氏名を入力してください。'
  else if (employeeName.length > 100) errors.employeeName = '氏名は100文字以内で入力してください。'
  if (!isIsoDate(values.birthDate)) errors.birthDate = values.birthDate ? '生年月日の形式が正しくありません。' : '生年月日を入力してください。'
  else if (values.birthDate > today()) errors.birthDate = '生年月日には未来日を入力できません。'
  if (!postalCode) errors.postalCode = '郵便番号を入力してください。'
  else if (!/^\d{7}$/.test(postalCode)) errors.postalCode = '郵便番号は半角数字7桁で入力してください。'
  if (!address) errors.address = '住所を入力してください。'
  else if (address.length > 255) errors.address = '住所は255文字以内で入力してください。'
  if (phoneNumber && !/^\d{1,20}$/.test(phoneNumber)) errors.phoneNumber = '電話番号は半角数字20桁以内で入力してください。'
  if (emailAddress && (emailAddress.length > 255 || !isEmailAddress(emailAddress))) errors.emailAddress = 'メールアドレスの形式が正しくありません。'
  if (requiresHireDate && !isIsoDate(values.hireDate)) errors.hireDate = values.hireDate ? '入社年月日の形式が正しくありません。' : '入社年月日を入力してください。'
  if (values.retireDate && !isIsoDate(values.retireDate)) errors.retireDate = '退職年月日の形式が正しくありません。'
  else if (values.retireDate && values.hireDate && values.retireDate < values.hireDate) errors.retireDate = '退職年月日は入社年月日以降の日付を入力してください。'
  if (!isPositiveInteger(values.departmentId)) errors.departmentId = '所属部署を選択してください。'
  if (!isPositiveInteger(values.skillGrade)) errors.skillGrade = '職能資格を選択してください。'

  const qualificationIds = new Set<string>()
  values.qualifications.forEach((qualification, index) => {
    if (!isPositiveInteger(qualification.qualificationId)) errors.qualifications[index].qualificationId = '資格を選択してください。'
    else if (qualificationIds.has(qualification.qualificationId)) errors.qualifications[index].qualificationId = '同一の資格を重複して指定できません。'
    else qualificationIds.add(qualification.qualificationId)
    if (!isIsoDate(qualification.acquisitionDate)) {
      errors.qualifications[index].acquisitionDate = '取得日を入力してください。'
    }
  })
  return errors
}

export function hasEmployeeFormErrors(errors: EmployeeFormErrors) {
  return Object.entries(errors).some(([key, value]) => {
    if (key === 'qualifications') return Array.isArray(value) && value.some((qualification) => Boolean(qualification.qualificationId || qualification.acquisitionDate))
    return Boolean(value)
  })
}

export function toEmployeeFormRequest(values: EmployeeFormValues): EmployeeFormRequest {
  return {
    employeeName: values.employeeName.trim(),
    birthDate: values.birthDate,
    postalCode: values.postalCode.trim(),
    address: values.address.trim(),
    phoneNumber: toNullable(values.phoneNumber),
    emailAddress: toNullable(values.emailAddress),
    departmentId: Number(values.departmentId),
    positionId: toNullableNumber(values.positionId),
    skillGrade: Number(values.skillGrade),
    retireDate: toNullable(values.retireDate),
    qualifications: values.qualifications.map((qualification) => ({
      qualificationId: Number(qualification.qualificationId),
      acquisitionDate: qualification.acquisitionDate,
    })),
  }
}

export function today() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export function nextMonthStart() {
  const now = new Date()
  return `${now.getFullYear() + (now.getMonth() === 11 ? 1 : 0)}-${String((now.getMonth() + 1) % 12 + 1).padStart(2, '0')}-01`
}

function isIsoDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
}

function isPositiveInteger(value: string) {
  return /^\d+$/.test(value) && Number.isSafeInteger(Number(value)) && Number(value) > 0
}

function isEmailAddress(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function toNullable(value: string) {
  const normalized = value.trim()
  return normalized || null
}

function toNullableNumber(value: string) {
  return value.trim() ? Number(value) : null
}
