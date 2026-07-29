import axios from 'axios'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { COMMON_MESSAGES } from '../../messages/commonMessages'
import { EmployeeForm } from './EmployeeSharedForm'
import { fetchEmployeeDetail, updateEmployee } from './employeeApi'
import { nextMonthStart, toEmployeeFormRequest, toEmployeeFormValues, today } from './employeeForm'
import {
  fetchEffectiveDepartments,
  fetchEffectivePositions,
  fetchEffectiveQualifications,
  fetchEffectiveSkillGrades,
  type EmployeeMasterOptions,
} from './employeeMasterApi'
import './employeeForm.css'
import type { ApiErrorResponse, EmployeeDetail, EmployeeFormValues } from './types'

const EMPTY_MASTER_OPTIONS: EmployeeMasterOptions = { departments: [], positions: [], qualifications: [], skillGrades: [] }

export function EmployeeEditPage() {
  const { employeeNo } = useParams()
  const validEmployeeNo = employeeNo && /^\d{1,20}$/.test(employeeNo) ? employeeNo : null
  const navigate = useNavigate()
  const [employee, setEmployee] = useState<EmployeeDetail | null>(null)
  const [masterOptions, setMasterOptions] = useState<EmployeeMasterOptions>(EMPTY_MASTER_OPTIONS)
  const [isLoadingEmployee, setIsLoadingEmployee] = useState(() => Boolean(validEmployeeNo))
  const [isLoadingMasters, setIsLoadingMasters] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [masterError, setMasterError] = useState('')
  const [optionalMasterNotices, setOptionalMasterNotices] = useState<string[]>([])
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const detailAbortControllerRef = useRef<AbortController | null>(null)
  const masterAbortControllerRef = useRef<AbortController | null>(null)
  const isMountedRef = useRef(true)

  async function loadMasterOptions(detailController: AbortController) {
    const controller = new AbortController()
    masterAbortControllerRef.current?.abort()
    masterAbortControllerRef.current = controller
    setIsLoadingMasters(true)
    setMasterError('')
    setOptionalMasterNotices([])
    setMasterOptions(EMPTY_MASTER_OPTIONS)

    try {
      const [departmentResult, skillGradeResult, positionResult, qualificationResult] = await Promise.allSettled([
        fetchEffectiveDepartments(nextMonthStart(), controller.signal),
        fetchEffectiveSkillGrades(nextMonthStart(), controller.signal),
        fetchEffectivePositions(nextMonthStart(), controller.signal),
        fetchEffectiveQualifications(today(), controller.signal),
      ])
      if (controller.signal.aborted || detailController.signal.aborted || masterAbortControllerRef.current !== controller) return

      const departments = departmentResult.status === 'fulfilled' ? departmentResult.value : []
      const skillGrades = skillGradeResult.status === 'fulfilled' ? skillGradeResult.value : []
      const positions = positionResult.status === 'fulfilled' ? positionResult.value : []
      const qualifications = qualificationResult.status === 'fulfilled' ? qualificationResult.value : []
      setMasterOptions({ departments, positions, qualifications, skillGrades })
      setMasterError(getRequiredMasterError(departmentResult, skillGradeResult, departments.length, skillGrades.length))
      setOptionalMasterNotices([
        getOptionalMasterNotice('役職', '役職なし', positionResult, positions.length),
        getOptionalMasterNotice('資格', '保有資格なし', qualificationResult, qualifications.length),
      ].filter((notice): notice is string => Boolean(notice)))
    } finally {
      if (!controller.signal.aborted && !detailController.signal.aborted && masterAbortControllerRef.current === controller) {
        masterAbortControllerRef.current = null
        setIsLoadingMasters(false)
      }
    }
  }

  useEffect(() => {
    const detailEmployeeNo = validEmployeeNo ?? ''
    if (!detailEmployeeNo) return
    const controller = new AbortController()
    detailAbortControllerRef.current?.abort()
    detailAbortControllerRef.current = controller

    async function loadEmployee() {
      setIsLoadingEmployee(true)
      setDetailError('')
      setEmployee(null)
      try {
        const detail = await fetchEmployeeDetail(detailEmployeeNo, controller.signal)
        if (controller.signal.aborted || detailAbortControllerRef.current !== controller) return
        setEmployee(detail)
        void loadMasterOptions(controller)
      } catch (error: unknown) {
        if (!controller.signal.aborted && detailAbortControllerRef.current === controller && !isRequestCancelled(error)) {
          setDetailError(getDetailErrorMessage(error))
        }
      } finally {
        if (!controller.signal.aborted && detailAbortControllerRef.current === controller) {
          detailAbortControllerRef.current = null
          setIsLoadingEmployee(false)
        }
      }
    }

    void loadEmployee()
    return () => controller.abort()
  }, [validEmployeeNo])

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
      detailAbortControllerRef.current?.abort()
      masterAbortControllerRef.current?.abort()
      detailAbortControllerRef.current = null
      masterAbortControllerRef.current = null
    }
  }, [])

  const invalidSelections = useMemo(() => {
    if (!employee || isLoadingMasters) return []
    const messages: string[] = []
    if (!masterOptions.departments.some((item) => item.departmentId === employee.departmentId)) messages.push('所属部署を入社日時点で有効な候補から再選択してください。')
    if (!masterOptions.skillGrades.some((item) => item.skillGrade === employee.skillGrade)) messages.push('職能資格を入社日時点で有効な候補から再選択してください。')
    if (employee.positionId !== null && !masterOptions.positions.some((item) => item.positionId === employee.positionId)) messages.push('現在の役職は有効候補にありません。役職なしまたは有効な役職を選択してください。')
    if (employee.qualifications.some((item) => !masterOptions.qualifications.some((option) => option.qualificationId === item.qualificationId))) messages.push('現在の保有資格に有効候補外の資格があります。削除または有効な資格へ変更してください。')
    return messages
  }, [employee, isLoadingMasters, masterOptions])

  const displayMasterOptions = useMemo(() => employee ? {
    departments: addCurrentDepartment(masterOptions.departments, employee),
    positions: addCurrentPosition(masterOptions.positions, employee),
    qualifications: addCurrentQualifications(masterOptions.qualifications, employee),
    skillGrades: addCurrentSkillGrade(masterOptions.skillGrades, employee),
  } : EMPTY_MASTER_OPTIONS, [employee, masterOptions])

  const handleSubmit = async (values: EmployeeFormValues) => {
    if (!employee || !validEmployeeNo) return
    const unresolvedMessage = getUnresolvedSelectionMessage(values, masterOptions, Boolean(masterError))
    if (unresolvedMessage) {
      setSubmitError(unresolvedMessage)
      return
    }

    setSubmitError('')
    setIsSubmitting(true)
    try {
      await updateEmployee(validEmployeeNo, toEmployeeFormRequest(values))
      navigate(ROUTES.employeeDetail.replace(':employeeNo', encodeURIComponent(validEmployeeNo)), {
        state: { successMessage: '社員情報を更新しました。' },
      })
    } catch (error: unknown) {
      if (isMountedRef.current) setSubmitError(getUpdateErrorMessage(error))
    } finally {
      if (isMountedRef.current) setIsSubmitting(false)
    }
  }

  const masterSelectionDisabled = isLoadingMasters || Boolean(masterError)
  return <section className="employee-form-page" aria-labelledby="employee-edit-title">
    <h2 id="employee-edit-title" className="employee-list-title">社員更新</h2>
    {!validEmployeeNo ? <p className="employee-api-message" role="alert">社員番号の形式が正しくありません。</p>
      : isLoadingEmployee ? <p className="employee-loading-message" role="status">社員情報を読み込み中です...</p>
        : detailError ? <p className="employee-api-message" role="alert">{detailError}</p>
          : employee && <>
            <section className="employee-readonly-info" aria-label="更新対象社員情報">
              <dl>
                <div><dt>社員番号</dt><dd>{employee.employeeNo}</dd></div>
                <div><dt>入社日</dt><dd>{employee.hireDate}</dd></div>
              </dl>
            </section>
            {isLoadingMasters && <p className="employee-form-guidance" role="status">更新時点の選択肢を読み込み中です...</p>}
            {masterError && <p className="employee-api-message" role="alert">{masterError}</p>}
            {optionalMasterNotices.map((notice) => <p key={notice} className="employee-form-guidance" role="status">{notice}</p>)}
            {invalidSelections.map((message) => <p key={message} className="employee-api-message" role="alert">{message}</p>)}
            <EmployeeForm
              departments={displayMasterOptions.departments}
              initialValues={toEmployeeFormValues(employee)}
              isMasterSelectionDisabled={masterSelectionDisabled}
              isSubmitting={isSubmitting}
              onCancel={() => navigate(ROUTES.employeeDetail.replace(':employeeNo', encodeURIComponent(employee.employeeNo)))}
              onSubmit={handleSubmit}
              positions={displayMasterOptions.positions}
              qualifications={displayMasterOptions.qualifications}
              requiresHireDate={false}
              skillGrades={displayMasterOptions.skillGrades}
              submitError={submitError}
              submitLabel="更新する"
            />
          </>}
  </section>
}

function addCurrentDepartment(options: EmployeeMasterOptions['departments'], employee: EmployeeDetail) {
  return options.some((item) => item.departmentId === employee.departmentId)
    ? options
    : [{ departmentId: employee.departmentId, departmentName: `現在の部署（再選択が必要）`, startDate: '', endDate: null }, ...options]
}

function addCurrentPosition(options: EmployeeMasterOptions['positions'], employee: EmployeeDetail) {
  if (employee.positionId === null || options.some((item) => item.positionId === employee.positionId)) return options
  return [{ positionId: employee.positionId, positionName: '現在の役職（再選択が必要）', positionAllowance: 0, startDate: '', endDate: null }, ...options]
}

function addCurrentSkillGrade(options: EmployeeMasterOptions['skillGrades'], employee: EmployeeDetail) {
  return options.some((item) => item.skillGrade === employee.skillGrade)
    ? options
    : [{ skillGrade: employee.skillGrade, allowance: 0, startDate: '', endDate: null, displayLabel: `${employee.skillGrade}級（再選択が必要）` }, ...options]
}

function addCurrentQualifications(options: EmployeeMasterOptions['qualifications'], employee: EmployeeDetail) {
  const missingOptions = employee.qualifications
    .filter((qualification) => !options.some((item) => item.qualificationId === qualification.qualificationId))
    .map((qualification) => ({
      qualificationId: qualification.qualificationId,
      qualificationName: `現在の資格 ID: ${qualification.qualificationId}（削除または変更が必要）`,
      isAdvance: false,
      qualificationAllowance: 0,
      startDate: '',
      endDate: null,
    }))
  return [...missingOptions, ...options]
}

function getUnresolvedSelectionMessage(values: EmployeeFormValues, options: EmployeeMasterOptions, requiredMasterUnavailable: boolean) {
  if (requiredMasterUnavailable) return '有効な部署および職能資格を取得できるまで更新できません。'
  if (!options.departments.some((item) => String(item.departmentId) === values.departmentId)
    || !options.skillGrades.some((item) => String(item.skillGrade) === values.skillGrade)) {
    return '所属部署と職能資格を有効な候補から選択してください。'
  }
  if (values.positionId && !options.positions.some((item) => String(item.positionId) === values.positionId)) {
    return '役職を有効な候補から選択するか、役職なしに変更してください。'
  }
  if (values.qualifications.some((qualification) => !options.qualifications.some((item) => String(item.qualificationId) === qualification.qualificationId))) {
    return '保有資格の有効候補外の項目を削除または変更してください。'
  }
  return ''
}

function getRequiredMasterError(
  departmentResult: PromiseSettledResult<unknown>,
  skillGradeResult: PromiseSettledResult<unknown>,
  departmentCount: number,
  skillGradeCount: number,
) {
  const failures = [departmentResult, skillGradeResult]
    .filter((result): result is PromiseRejectedResult => result.status === 'rejected')
    .map((result) => result.reason)
  if (departmentCount === 0 || skillGradeCount === 0 || failures.some(isNoCandidateError)) {
    return '更新時点で有効な部署または職能資格がありません。更新時点を確認してください。'
  }
  if (failures.some(isServerError)) return COMMON_MESSAGES.serverError
  return failures.length > 0 ? COMMON_MESSAGES.networkError : ''
}

function getOptionalMasterNotice(masterName: string, emptySelectionLabel: string, result: PromiseSettledResult<unknown>, count: number) {
  if (result.status === 'fulfilled' && count > 0) return ''
  if (result.status === 'fulfilled' || isNoCandidateError(result.reason)) {
    return `更新時点で有効な${masterName}はありません。${emptySelectionLabel}で更新できます。`
  }
  return isServerError(result.reason)
    ? `${COMMON_MESSAGES.serverError} ${emptySelectionLabel}で更新できます。`
    : `${COMMON_MESSAGES.networkError} ${emptySelectionLabel}で更新できます。`
}

function isRequestCancelled(error: unknown) {
  return axios.isCancel(error) || (axios.isAxiosError(error) && error.code === 'ERR_CANCELED')
}

function isNoCandidateError(error: unknown) {
  return axios.isAxiosError(error) && (error.response?.status === 400 || error.response?.status === 404)
}

function isServerError(error: unknown) {
  return axios.isAxiosError(error) && Boolean(error.response && error.response.status >= 500)
}

function getDetailErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400) return error.response.data?.message ?? COMMON_MESSAGES.networkError
    if (error.response?.status === 404) return error.response.data?.message ?? '対象社員が存在しない、または更新対象外です。'
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return COMMON_MESSAGES.networkError
}

function getUpdateErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400) return error.response.data?.message ?? COMMON_MESSAGES.networkError
    if (error.response?.status === 404) return error.response.data?.message ?? '対象社員が存在しない、または更新対象外です。'
    if (error.response?.status === 409) return '他の操作により社員情報が更新されています。最新の情報を再読み込みしてから更新してください。'
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return COMMON_MESSAGES.networkError
}
