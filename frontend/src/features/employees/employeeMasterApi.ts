import { apiClient } from '../../lib/apiClient'
import type { Department, Position, Qualification, SkillGrade } from '../master/types'

export type EmployeeMasterOptions = {
  departments: Department[]
  positions: Position[]
  qualifications: Qualification[]
  skillGrades: SkillGrade[]
}

export async function fetchEmployeeMasterOptions(targetDate: string, signal?: AbortSignal): Promise<EmployeeMasterOptions> {
  const [departments, positions, qualifications, skillGrades] = await Promise.all([
    fetchEffectiveDepartments(targetDate, signal),
    fetchEffectivePositions(targetDate, signal),
    fetchEffectiveQualifications(targetDate, signal),
    fetchEffectiveSkillGrades(targetDate, signal),
  ])

  return { departments, positions, qualifications, skillGrades }
}

export async function fetchQualificationNamesAtAcquisitionDates(
  acquisitionDates: string[],
  signal?: AbortSignal,
) {
  const uniqueDates = [...new Set(acquisitionDates)]
  const results = await Promise.allSettled(uniqueDates.map(async (acquisitionDate) => ({
    acquisitionDate,
    qualifications: await fetchEffectiveQualifications(acquisitionDate, signal),
  })))
  const names = new Map<string, Map<number, string>>()
  let hasFailure = false
  results.forEach((result) => {
    if (result.status !== 'fulfilled') {
      hasFailure = true
      return
    }
    names.set(
      result.value.acquisitionDate,
      new Map(result.value.qualifications.map((qualification) => [qualification.qualificationId, qualification.qualificationName])),
    )
  })
  return { names, hasFailure }
}

export async function fetchEffectiveDepartments(targetDate: string, signal?: AbortSignal) {
  const departments = await fetchOptions<Department>(`/department/${targetDate}`, isDepartment, signal)
  return departments.map((department) => ({ ...department, departmentName: department.departmentName.trim() }))
}

export async function fetchEffectivePositions(targetDate: string, signal?: AbortSignal) {
  const positions = await fetchOptions<Position>(`/position/${targetDate}`, isPosition, signal)
  return positions.map((position) => ({ ...position, positionName: position.positionName.trim() }))
}

export async function fetchEffectiveQualifications(targetDate: string, signal?: AbortSignal) {
  const qualifications = await fetchOptions<Qualification>(`/qualification/${targetDate}`, isQualification, signal)
  return qualifications.map((qualification) => ({ ...qualification, qualificationName: qualification.qualificationName.trim() }))
}

export async function fetchEffectiveSkillGrades(targetDate: string, signal?: AbortSignal) {
  return fetchOptions<SkillGrade>(`/skillgrade/${targetDate}`, isSkillGrade, signal)
}

async function fetchOptions<T>(url: string, isOption: (value: unknown) => value is T, signal?: AbortSignal) {
  const response = await apiClient.get<unknown>(url, { signal })
  if (!Array.isArray(response.data)) throw new Error('マスタAPIのレスポンス形式が正しくありません。')
  return response.data.filter(isOption)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function isDepartment(value: unknown): value is Department {
  return isRecord(value)
    && isPositiveInteger(value.departmentId)
    && isName(value.departmentName)
    && isIsoDate(value.startDate)
    && (isIsoDate(value.endDate) || value.endDate === null)
}

function isPosition(value: unknown): value is Position {
  return isRecord(value)
    && isPositiveInteger(value.positionId)
    && isName(value.positionName)
    && isFiniteNumber(value.positionAllowance)
    && isIsoDate(value.startDate)
    && (isIsoDate(value.endDate) || value.endDate === null)
}

function isQualification(value: unknown): value is Qualification {
  return isRecord(value)
    && isPositiveInteger(value.qualificationId)
    && isName(value.qualificationName)
    && typeof value.isAdvance === 'boolean'
    && isFiniteNumber(value.qualificationAllowance)
    && isIsoDate(value.startDate)
    && (isIsoDate(value.endDate) || value.endDate === null)
}

function isSkillGrade(value: unknown): value is SkillGrade {
  return isRecord(value)
    && isPositiveInteger(value.skillGrade)
    && isFiniteNumber(value.allowance)
    && isIsoDate(value.startDate)
    && (isIsoDate(value.endDate) || value.endDate === null)
}

function isPositiveInteger(value: unknown) {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}

function isFiniteNumber(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value)
}

function isName(value: unknown) {
  return typeof value === 'string' && value.trim().length > 0
}

function isIsoDate(value: unknown) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
}
