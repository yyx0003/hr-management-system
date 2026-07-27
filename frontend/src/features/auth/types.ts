export type AuthUser = {
  employeeId: number
  employeeNo: string
  employeeName: string
}

export type LoginRequest = {
  employeeNo: string
  password: string
}

export type LoginResponse = AuthUser & {
  token: string
}

export type ApiErrorResponse = {
  message?: string
}
