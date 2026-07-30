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

export type EmployeeQualification = {
  qualificationId: number
  acquisitionDate: string
}

export type EmployeeDetail = {
  employeeId: number
  employeeNo: string
  employeeName: string
  departmentId: number
  birthDate: string
  postalCode: string
  address: string
  phoneNumber: string | null
  emailAddress: string | null
  hireDate: string
  retireDate: string | null
  positionId: number | null
  skillGrade: number
  qualifications: EmployeeQualification[]
}

export type EmployeeCreateResponse = {
  employeeId: number
  employeeNo: string
}

export type EmployeeUpdateResponse = EmployeeCreateResponse

export type EmployeeFormValues = {
  employeeName: string
  birthDate: string
  postalCode: string
  address: string
  phoneNumber: string
  emailAddress: string
  hireDate: string
  retireDate: string
  departmentId: string
  positionId: string
  skillGrade: string
  qualifications: Array<{
    qualificationId: string
    acquisitionDate: string
  }>
}

export type EmployeeFormRequest = {
  employeeName: string
  birthDate: string
  postalCode: string
  address: string
  phoneNumber: string | null
  emailAddress: string | null
  departmentId: number
  positionId: number | null
  skillGrade: number
  retireDate: string | null
  qualifications: EmployeeQualification[]
}

export type EmployeeCreateRequest = EmployeeFormRequest & {
  hireDate: string
}

export type EmployeeUpdateRequest = EmployeeFormRequest
