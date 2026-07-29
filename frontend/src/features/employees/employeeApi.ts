import { apiClient } from '../../lib/apiClient'
import type { DepartmentOption, EmployeeListItem, EmployeeSearchCondition } from './types'

export async function searchEmployees(condition: EmployeeSearchCondition, signal?: AbortSignal) {
  const params = toSearchParams(condition)
  const response = await apiClient.get<EmployeeListItem[]>('/api/employees', { params, signal })
  return response.data
}

// The department endpoint is intentionally isolated here because its contract is pending backend merge.
export async function fetchActiveDepartments(targetDate: string): Promise<DepartmentOption[]> {
  const response = await apiClient.get<unknown>(`/department/${targetDate}`)
  if (!Array.isArray(response.data)) return []

  return response.data
    .filter(isDepartmentOption)
    .map((department) => ({
      departmentId: department.departmentId,
      departmentName: department.departmentName.trim(),
    }))
}

function isDepartmentOption(value: unknown): value is DepartmentOption {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false

  const department = value as Record<string, unknown>
  return (
    typeof department.departmentId === 'number' &&
    Number.isSafeInteger(department.departmentId) &&
    department.departmentId > 0 &&
    typeof department.departmentName === 'string' &&
    department.departmentName.trim().length > 0
  )
}

function toSearchParams(condition: EmployeeSearchCondition) {
  const employeeNo = condition.employeeNo.trim()
  const employeeName = condition.employeeName.trim()
  const departmentId = condition.departmentId.trim()

  return {
    ...(employeeNo ? { employeeNo } : {}),
    ...(employeeName ? { employeeName } : {}),
    ...(departmentId ? { departmentId } : {}),
  }
}
