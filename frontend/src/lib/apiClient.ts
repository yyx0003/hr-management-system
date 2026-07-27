import axios from 'axios'
import { ROUTES } from '../constants/routes'
import { getAccessToken, removeAuth } from '../features/auth/authStorage'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

apiClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isLoginRequest(error.config?.url)) {
      removeAuth()
      if (window.location.pathname !== ROUTES.login) {
        window.location.assign(ROUTES.login)
      }
    }
    return Promise.reject(error)
  },
)

function isLoginRequest(url: string | undefined) {
  return url === '/api/auth/login' || url?.endsWith('/api/auth/login')
}
