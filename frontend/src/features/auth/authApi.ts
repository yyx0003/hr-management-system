import { apiClient } from '../../lib/apiClient'
import type { LoginRequest, LoginResponse } from './types'

export async function login(request: LoginRequest) {
  const response = await apiClient.post<LoginResponse>('/api/auth/login', request)
  return response.data
}
