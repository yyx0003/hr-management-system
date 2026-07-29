import axios from 'axios'
import { type FormEvent, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { COMMON_MESSAGES } from '../../messages/commonMessages'
import { fetchActiveDepartments, searchEmployees } from './employeeApi'
import { triggerRetireeDelete } from '../../api/batch'
import './employeeList.css'
import { EMPLOYEE_MESSAGES } from './messages'
import type { ApiErrorResponse, DepartmentOption, EmployeeListItem, EmployeeSearchCondition } from './types'

const initialCondition: EmployeeSearchCondition = {
  employeeNo: '',
  employeeName: '',
  departmentId: '',
}

export function EmployeeListPage() {
  const navigate = useNavigate()
  const [condition, setCondition] = useState<EmployeeSearchCondition>(initialCondition)
  const [departments, setDepartments] = useState<DepartmentOption[]>([])
  const [employees, setEmployees] = useState<EmployeeListItem[]>([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, setIsSearching] = useState(false)
  const [isLoadingDepartments, setIsLoadingDepartments] = useState(true)
  const [selectedEmployeeNo, setSelectedEmployeeNo] = useState<string | null>(null)
  const [employeeNoError, setEmployeeNoError] = useState('')
  const [employeeNameError, setEmployeeNameError] = useState('')
  const [departmentIdError, setDepartmentIdError] = useState('')
  const [departmentLoadError, setDepartmentLoadError] = useState('')
  const [searchError, setSearchError] = useState('')
  const [selectionError, setSelectionError] = useState('')
  const [isRetiring, setIsRetiring] = useState(false)
const [retireResult, setRetireResult] = useState('')
  const searchAbortControllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    let isCancelled = false

    async function loadDepartments() {
      try {
        const activeDepartments = await fetchActiveDepartments(getToday())
        if (!isCancelled) setDepartments(activeDepartments)
      } catch (error: unknown) {
        if (!isCancelled) setDepartmentLoadError(getApiErrorMessage(error))
      } finally {
        if (!isCancelled) setIsLoadingDepartments(false)
      }
    }

    void loadDepartments()
    return () => {
      isCancelled = true
    }
  }, [])

  useEffect(() => () => {
    searchAbortControllerRef.current?.abort()
    searchAbortControllerRef.current = null
  }, [])

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSearching) return

    searchAbortControllerRef.current?.abort()
    searchAbortControllerRef.current = null

    const errors = validate(condition)
    setEmployees([])
    setHasSearched(false)
    setSelectedEmployeeNo(null)
    setSelectionError('')
    setSearchError('')
    setEmployeeNoError(errors.employeeNo)
    setEmployeeNameError(errors.employeeName)
    setDepartmentIdError(errors.departmentId)

    if (errors.employeeNo || errors.employeeName || errors.departmentId) return

    const controller = new AbortController()
    searchAbortControllerRef.current = controller
    setIsSearching(true)

    try {
      const result = await searchEmployees(condition, controller.signal)
      if (searchAbortControllerRef.current !== controller) return

      setEmployees(result)
      setHasSearched(true)
    } catch (error: unknown) {
      if (searchAbortControllerRef.current !== controller || isRequestCancelled(error)) return

      setSearchError(getApiErrorMessage(error))
    } finally {
      if (searchAbortControllerRef.current === controller) {
        searchAbortControllerRef.current = null
        setIsSearching(false)
      }
    }
  }

  const handleUpdate = () => {
    if (!selectedEmployeeNo) {
      setSelectionError(EMPLOYEE_MESSAGES.selectRequired)
      return
    }

    navigate(ROUTES.employeeEdit.replace(':employeeNo', encodeURIComponent(selectedEmployeeNo)))
  }
  const handleRetireeDeleteTest = async () => {
  setIsRetiring(true)
  setRetireResult('')
  try {
    const result = await triggerRetireeDelete()
    setRetireResult(`${result.status}: ${result.message}`)
  } catch (error: unknown) {
    setRetireResult(getApiErrorMessage(error))
  } finally {
    setIsRetiring(false)
  }
}

  const departmentDescribedBy = getDescribedBy(
    isLoadingDepartments ? 'department-loading' : '',
    departmentLoadError ? 'department-load-error' : '',
    departmentIdError ? 'department-id-error' : '',
  )

  return (
    <section className="employee-list-page" aria-labelledby="employee-list-title">
      <h2 id="employee-list-title" className="employee-list-title">社員一覧</h2>

      <form className="employee-search-form" noValidate onSubmit={handleSearch}>
        <h3 className="employee-card-title">検索条件</h3>
        <div className="employee-search-fields">
          <label className="employee-search-field" htmlFor="employee-no">
            <span>社員番号</span>
            <input
              aria-describedby={employeeNoError ? 'employee-no-error' : undefined}
              aria-invalid={Boolean(employeeNoError)}
              id="employee-no"
              inputMode="numeric"
              maxLength={20}
              name="employeeNo"
              value={condition.employeeNo}
              onChange={(event) => {
                setCondition({ ...condition, employeeNo: event.target.value })
                setEmployeeNoError('')
              }}
            />
            <span className="employee-field-messages">
              {employeeNoError && <span className="employee-field-error" id="employee-no-error">{employeeNoError}</span>}
            </span>
          </label>

          <label className="employee-search-field" htmlFor="employee-name">
            <span>氏名</span>
            <input
              aria-describedby={employeeNameError ? 'employee-name-error' : undefined}
              aria-invalid={Boolean(employeeNameError)}
              id="employee-name"
              maxLength={101}
              name="employeeName"
              value={condition.employeeName}
              onChange={(event) => {
                setCondition({ ...condition, employeeName: event.target.value })
                setEmployeeNameError('')
              }}
            />
            <span className="employee-field-messages">
              {employeeNameError && <span className="employee-field-error" id="employee-name-error">{employeeNameError}</span>}
            </span>
          </label>

          <label className="employee-search-field" htmlFor="department-id">
            <span>所属部署</span>
            <select
              aria-describedby={departmentDescribedBy}
              aria-invalid={Boolean(departmentIdError)}
              disabled={isLoadingDepartments}
              id="department-id"
              name="departmentId"
              value={condition.departmentId}
              onChange={(event) => {
                setCondition({ ...condition, departmentId: event.target.value })
                setDepartmentIdError('')
              }}
            >
              <option value="">未選択</option>
              {departments.map((department) => (
                <option key={department.departmentId} value={department.departmentId}>
                  {department.departmentName}
                </option>
              ))}
            </select>
            <span className="employee-field-messages">
              {isLoadingDepartments && <span id="department-loading" role="status">{EMPLOYEE_MESSAGES.departmentLoading}</span>}
              {departmentLoadError && <span className="employee-field-error" id="department-load-error" role="alert">{departmentLoadError}</span>}
              {departmentIdError && <span className="employee-field-error" id="department-id-error">{departmentIdError}</span>}
            </span>
          </label>
        </div>
        <div className="employee-form-actions">
          <button className="employee-primary-button" disabled={isSearching} type="submit">
            {isSearching ? '検索中...' : '検索'}
          </button>
        </div>
      </form>

      <section className="employee-result-section" aria-labelledby="employee-result-title">
        <div className="employee-result-heading">
          <h3 id="employee-result-title" className="employee-card-title">検索結果</h3>
          {hasSearched && !isSearching && <span className="employee-result-count">（{employees.length}件）</span>}
        </div>
        {searchError && <p className="employee-api-message" role="alert">{searchError}</p>}
        <div className="employee-table-wrapper">
          <table className="employee-table">
            <thead>
              <tr>
                <th className="employee-select-cell" scope="col">選択</th>
                <th scope="col">社員番号</th>
                <th scope="col">氏名</th>
                <th scope="col">所属部署</th>
                <th scope="col">役職</th>
                <th scope="col">職能資格</th>
                <th scope="col">入社年月日</th>
              </tr>
            </thead>
            <tbody>
              {isSearching ? (
                <MessageRow className="employee-loading-message" message="検索中です。" />
              ) : !hasSearched ? (
                <MessageRow message="検索条件を指定し、検索ボタンを押してください。" />
              ) : employees.length === 0 ? (
                <MessageRow className="employee-empty-message" message={EMPLOYEE_MESSAGES.searchNotFound} />
              ) : (
                employees.map((employee) => (
                  <tr
                    key={employee.employeeId}
                    className={`employee-row${selectedEmployeeNo === employee.employeeNo ? ' employee-row-selected' : ''}`}
                  >
                    <td className="employee-select-cell">
                      <input
                        aria-label={`${employee.employeeNo} ${employee.employeeName} を選択`}
                        checked={selectedEmployeeNo === employee.employeeNo}
                        name="selectedEmployee"
                        type="radio"
                        value={employee.employeeNo}
                        onChange={() => {
                          setSelectedEmployeeNo(employee.employeeNo)
                          setSelectionError('')
                        }}
                      />
                    </td>
                    <td>{employee.employeeNo}</td>
                    <td>{employee.employeeName}</td>
                    <td>{employee.departmentName}</td>
                    <td>{employee.positionName ?? EMPLOYEE_MESSAGES.noPosition}</td>
                    <td>{employee.skillGrade}等級</td>
                    <td>{employee.hireDate}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {selectionError && <p className="employee-api-message" role="alert">{selectionError}</p>}
        <div className="employee-list-actions">
          <button className="employee-primary-button" disabled={isSearching} type="button" onClick={handleUpdate}>
            更新画面へ
          </button>
          <button className="employee-secondary-button" type="button" onClick={() => navigate(ROUTES.menu)}>
            戻る
          </button>
        </div>
      </section>

      {/* ↓↓↓ 開発・テスト用（本番では削除予定） ↓↓↓ */}
      <section style={{ marginTop: '32px', padding: '12px', border: '1px dashed #999' }}>
        <button type="button" disabled={isRetiring} onClick={handleRetireeDeleteTest}>
          {isRetiring ? '実行中...' : '【テスト】退職者削除バッチ実行'}
        </button>
        {retireResult && <p>{retireResult}</p>}
      </section>
      {/* ↑↑↑ 開発・テスト用（本番では削除予定） ↑↑↑ */}
    </section>
  )
}

type MessageRowProps = {
  className?: string
  message: string
}

function MessageRow({ className, message }: MessageRowProps) {
  return (
    <tr>
      <td className="employee-table-message" colSpan={7}>
        <p className={className} role="status">{message}</p>
      </td>
    </tr>
  )
}

function validate(condition: EmployeeSearchCondition) {
  const employeeNo = condition.employeeNo.trim()
  const employeeName = condition.employeeName.trim()
  const departmentId = condition.departmentId.trim()

  return {
    employeeNo: employeeNo && (!/^\d+$/.test(employeeNo) || employeeNo.length > 20)
      ? EMPLOYEE_MESSAGES.employeeNoFormat
      : '',
    employeeName: employeeName.length > 100 ? EMPLOYEE_MESSAGES.employeeNameMaxLength : '',
    departmentId: departmentId && !isPositiveSafeInteger(departmentId)
      ? EMPLOYEE_MESSAGES.departmentIdInvalid
      : '',
  }
}

function isPositiveSafeInteger(value: string) {
  return /^\d+$/.test(value) && Number.isSafeInteger(Number(value)) && Number(value) > 0
}

function getToday() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDescribedBy(...ids: string[]) {
  const descriptions = ids.filter(Boolean)
  return descriptions.length > 0 ? descriptions.join(' ') : undefined
}

function isRequestCancelled(error: unknown) {
  return axios.isCancel(error) || (axios.isAxiosError(error) && error.code === 'ERR_CANCELED')
}

function getApiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400) {
      return error.response.data?.message ?? COMMON_MESSAGES.networkError
    }
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return COMMON_MESSAGES.networkError
}
