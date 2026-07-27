import axios from 'axios'
import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { login } from '../features/auth/authApi'
import { saveAuth } from '../features/auth/authStorage'
import { AUTH_MESSAGES } from '../features/auth/messages'
import type { ApiErrorResponse } from '../features/auth/types'
import { COMMON_MESSAGES } from '../messages/commonMessages'

export function LoginPage() {
  const navigate = useNavigate()
  const [employeeNo, setEmployeeNo] = useState('')
  const [password, setPassword] = useState('')
  const [employeeNoError, setEmployeeNoError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) return

    const nextEmployeeNoError = employeeNo.trim() ? '' : AUTH_MESSAGES.employeeNoRequired
    const nextPasswordError = password ? '' : AUTH_MESSAGES.passwordRequired

    setEmployeeNoError(nextEmployeeNoError)
    setPasswordError(nextPasswordError)
    setErrorMessage('')

    if (nextEmployeeNoError || nextPasswordError) return

    setIsSubmitting(true)

    try {
      const response = await login({ employeeNo, password })
      saveAuth(response)
      navigate(ROUTES.menu, { replace: true })
    } catch (error: unknown) {
      setErrorMessage(getLoginErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <div className="login-system-mark" aria-hidden="true" />
        <p className="login-system-name">社員情報管理システム</p>
        <h1 id="login-title" className="login-title">ログイン</h1>
        <form className="login-form" noValidate onSubmit={handleSubmit}>
          <label className="login-field" htmlFor="employee-no">
            <span>社員番号</span>
            <input
              autoComplete="username"
              aria-describedby={getDescribedBy(employeeNoError, errorMessage, 'employee-no-error')}
              aria-invalid={Boolean(employeeNoError)}
              id="employee-no"
              name="employeeNo"
              required
              value={employeeNo}
              onChange={(event) => {
                setEmployeeNo(event.target.value)
                setEmployeeNoError('')
              }}
            />
            {employeeNoError && <span className="login-field-error" id="employee-no-error">{employeeNoError}</span>}
          </label>
          <label className="login-field" htmlFor="password">
            <span>パスワード</span>
            <input
              autoComplete="current-password"
              aria-describedby={getDescribedBy(passwordError, errorMessage, 'password-error')}
              aria-invalid={Boolean(passwordError)}
              id="password"
              name="password"
              required
              type="password"
              value={password}
              onChange={(event) => {
                setPassword(event.target.value)
                setPasswordError('')
              }}
            />
            {passwordError && <span className="login-field-error" id="password-error">{passwordError}</span>}
          </label>
          {errorMessage && <p className="login-api-error" id="login-error" role="alert">{errorMessage}</p>}
          <button className="login-submit-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>
      </section>
    </main>
  )
}

function getDescribedBy(fieldError: string, formError: string, fieldErrorId: string) {
  const descriptions = [fieldError ? fieldErrorId : '', formError ? 'login-error' : ''].filter(Boolean)
  return descriptions.length > 0 ? descriptions.join(' ') : undefined
}

function getLoginErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400 || error.response?.status === 401) {
      return error.response.data?.message || AUTH_MESSAGES.loginFailed
    }
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return COMMON_MESSAGES.networkError
}
