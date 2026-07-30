import { apiClient } from '../lib/apiClient'

export interface RetireeDeleteResult {
  status: string
  message: string
}

export async function triggerRetireeDelete(): Promise<RetireeDeleteResult> {
  const response = await apiClient.post<RetireeDeleteResult>('/api/batch/retiree-delete')
  return response.data
}