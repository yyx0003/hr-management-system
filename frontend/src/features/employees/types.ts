export type EmployeeSearchCondition = {
  employeeNo: string
  employeeName: string
  departmentId: string
}

export type EmployeeListItem = {
  employeeId: number
  employeeNo: string
  employeeName: string
  departmentName: string
  positionName: string | null
  skillGrade: number
  hireDate: string
}

export type DepartmentOption = {
  departmentId: number
  departmentName: string
}

export type ApiErrorResponse = {
  message?: string
}
