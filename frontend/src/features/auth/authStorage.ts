import { STORAGE_KEYS } from '../../constants/storageKeys'
import type { AuthUser, LoginResponse } from './types'

let memoryAccessToken: string | null = null
let memoryAuthUser: AuthUser | null = null
let isSessionStorageUnavailable = false

export function saveAuth(loginResponse: LoginResponse) {
  const { token, employeeId, employeeNo, employeeName } = loginResponse
  const authUser: AuthUser = { employeeId, employeeNo, employeeName }

  memoryAccessToken = token
  memoryAuthUser = authUser

  try {
    sessionStorage.setItem(STORAGE_KEYS.accessToken, token)
    sessionStorage.setItem(STORAGE_KEYS.authUser, JSON.stringify(authUser))
    isSessionStorageUnavailable = false
  } catch {
    isSessionStorageUnavailable = true
    // sessionStorageが利用不能な場合は、メモリ上の認証情報を利用する。
  }
}

export function getAccessToken() {
  if (isSessionStorageUnavailable) return memoryAccessToken

  try {
    const accessToken = sessionStorage.getItem(STORAGE_KEYS.accessToken)
    if (accessToken !== null) memoryAccessToken = accessToken
    return accessToken
  } catch {
    isSessionStorageUnavailable = true
    return memoryAccessToken
  }
}

export function getAuthUser(): AuthUser | null {
  if (isSessionStorageUnavailable) return memoryAuthUser

  let value: string | null
  try {
    value = sessionStorage.getItem(STORAGE_KEYS.authUser)
  } catch {
    isSessionStorageUnavailable = true
    return memoryAuthUser
  }

  if (!value) return null

  try {
    const authUser: unknown = JSON.parse(value)
    if (isAuthUser(authUser)) {
      memoryAuthUser = authUser
      return authUser
    }
  } catch {
    // 不正なJSONは下で認証情報ごと削除する。
  }

  removeAuth()
  return null
}

export function removeAuth() {
  memoryAccessToken = null
  memoryAuthUser = null

  try {
    sessionStorage.removeItem(STORAGE_KEYS.accessToken)
    sessionStorage.removeItem(STORAGE_KEYS.authUser)
    isSessionStorageUnavailable = false
  } catch {
    isSessionStorageUnavailable = true
    // sessionStorageを利用できない場合も、画面を停止させない。
  }
}

function isAuthUser(value: unknown): value is AuthUser {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false

  const authUser = value as Record<string, unknown>
  return (
    typeof authUser.employeeId === 'number' &&
    typeof authUser.employeeNo === 'string' &&
    typeof authUser.employeeName === 'string'
  )
}
