import axios from 'axios'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { COMMON_MESSAGES } from '../../messages/commonMessages'
import { EmployeeForm } from './EmployeeSharedForm'
import { createEmployee } from './employeeApi'
import { isValidIsoDate, EMPTY_EMPLOYEE_FORM_VALUES, toEmployeeFormRequest } from './employeeForm'
import {
  fetchEffectiveDepartments,
  fetchEffectivePositions,
  fetchEffectiveQualifications,
  fetchEffectiveSkillGrades,
  type EmployeeMasterOptions,
} from './employeeMasterApi'
import './employeeForm.css'
import type { ApiErrorResponse, EmployeeCreateRequest, EmployeeFormValues } from './types'

const EMPTY_MASTER_OPTIONS: EmployeeMasterOptions = {
  departments: [],
  positions: [],
  qualifications: [],
  skillGrades: [],
}

export function EmployeeCreatePage() {
  const navigate = useNavigate()
  const [masterOptions, setMasterOptions] = useState<EmployeeMasterOptions>(EMPTY_MASTER_OPTIONS)
  const [isLoadingMasters, setIsLoadingMasters] = useState(false)
  const [masterError, setMasterError] = useState('')
  const [optionalMasterNotices, setOptionalMasterNotices] = useState<string[]>([])
  const [hireDate, setHireDate] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const masterAbortControllerRef = useRef<AbortController | null>(null)
  const isMountedRef = useRef(true)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
      masterAbortControllerRef.current?.abort()
      masterAbortControllerRef.current = null
    }
  }, [])

  const handleHireDateChange = (nextHireDate: string) => {
    setHireDate(nextHireDate)
    masterAbortControllerRef.current?.abort()
    masterAbortControllerRef.current = null
    setMasterOptions(EMPTY_MASTER_OPTIONS)
    setMasterError('')
    setOptionalMasterNotices([])

    if (!isValidIsoDate(nextHireDate)) {
      setIsLoadingMasters(false)
      return
    }

    const controller = new AbortController()
    masterAbortControllerRef.current = controller
    setIsLoadingMasters(true)
    void loadMasterOptions(nextHireDate, controller)
  }

  const loadMasterOptions = async (targetHireDate: string, controller: AbortController) => {
    try {
      const [departmentResult, skillGradeResult, positionResult, qualificationResult] = await Promise.allSettled([
        fetchEffectiveDepartments(targetHireDate, controller.signal),
        fetchEffectiveSkillGrades(targetHireDate, controller.signal),
        fetchEffectivePositions(targetHireDate, controller.signal),
        fetchEffectiveQualifications(targetHireDate, controller.signal),
      ])
      if (controller.signal.aborted || masterAbortControllerRef.current !== controller) return
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
    } catch (error: unknown) {
      if (controller.signal.aborted || masterAbortControllerRef.current !== controller || isRequestCancelled(error)) return
      setMasterError(getMasterErrorMessage(error))
    } finally {
      if (!controller.signal.aborted && masterAbortControllerRef.current === controller) {
        masterAbortControllerRef.current = null
        setIsLoadingMasters(false)
      }
    }
  }

  const handleSubmit = async (values: EmployeeFormValues) => {
    setSubmitError('')
    setIsSubmitting(true)
    try {
      const request: EmployeeCreateRequest = {
        ...toEmployeeFormRequest(values),
        hireDate: values.hireDate,
      }
      const created = await createEmployee(request)
      navigate(ROUTES.employeeDetail.replace(':employeeNo', encodeURIComponent(created.employeeNo)), {
        state: { successMessage: `社員を登録しました。社員番号：${created.employeeNo}` },
      })
    } catch (error: unknown) {
      if (isMountedRef.current) setSubmitError(getSubmitErrorMessage(error))
    } finally {
      if (isMountedRef.current) setIsSubmitting(false)
    }
  }

  const masterSelectionDisabled = !isValidIsoDate(hireDate) || isLoadingMasters || Boolean(masterError)
  const hasNoMasterOptions = isValidIsoDate(hireDate)
    && !isLoadingMasters
    && !masterError
    && (masterOptions.departments.length === 0 || masterOptions.skillGrades.length === 0)

  return <section className="employee-form-page" aria-labelledby="employee-create-title">
    <h2 id="employee-create-title" className="employee-list-title">社員登録</h2>
    <p className="employee-create-guidance">社員番号は登録時に自動採番されます。</p>
    {!isValidIsoDate(hireDate) && <p className="employee-form-guidance" role="status">入社日を入力すると、所属情報と保有資格の候補を選択できます。</p>}
    {isLoadingMasters && <p className="employee-form-guidance" role="status">入社日時点の選択肢を読み込み中です...</p>}
    {masterError && <p className="employee-api-message" role="alert">{masterError}</p>}
    {hasNoMasterOptions && <p className="employee-api-message" role="alert">入社日時点で選択できる部署または職能資格がありません。</p>}
    {optionalMasterNotices.map((notice) => <p key={notice} className="employee-form-guidance" role="status">{notice}</p>)}
    <EmployeeForm
      departments={masterOptions.departments}
      initialValues={EMPTY_EMPLOYEE_FORM_VALUES}
      isMasterSelectionDisabled={masterSelectionDisabled}
      isSubmitting={isSubmitting}
      onCancel={() => navigate(ROUTES.employees)}
      onHireDateChange={handleHireDateChange}
      onSubmit={handleSubmit}
      positions={masterOptions.positions}
      qualifications={masterOptions.qualifications}
      requiresHireDate
      skillGrades={masterOptions.skillGrades}
      submitError={submitError}
      submitLabel="登録する"
    />
  </section>
}

function isRequestCancelled(error: unknown) {
  return axios.isCancel(error) || (axios.isAxiosError(error) && error.code === 'ERR_CANCELED')
}

function getMasterErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400) return error.response.data?.message ?? '選択肢の取得に失敗しました。'
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return '選択肢の取得に失敗しました。入社日を確認して再度入力してください。'
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
    return '入社日時点で有効な部署または職能資格がありません。入社日を確認してください。'
  }
  if (failures.some(isServerError)) return COMMON_MESSAGES.serverError
  return failures.length > 0 ? COMMON_MESSAGES.networkError : ''
}

function getOptionalMasterNotice(
  masterName: string,
  emptySelectionLabel: string,
  result: PromiseSettledResult<unknown>,
  count: number,
) {
  if (result.status === 'fulfilled' && count > 0) return ''
  if (result.status === 'fulfilled' || isNoCandidateError(result.reason)) {
    return `入社日時点で有効な${masterName}はありません。${emptySelectionLabel}で登録できます。`
  }
  return isServerError(result.reason)
    ? `${COMMON_MESSAGES.serverError} ${emptySelectionLabel}で登録できます。`
    : `${COMMON_MESSAGES.networkError} ${emptySelectionLabel}で登録できます。`
}

function isNoCandidateError(error: unknown) {
  return axios.isAxiosError(error) && (error.response?.status === 400 || error.response?.status === 404)
}

function isServerError(error: unknown) {
  return axios.isAxiosError(error) && Boolean(error.response && error.response.status >= 500)
}

function getSubmitErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 400) return error.response.data?.message ?? COMMON_MESSAGES.networkError
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
  }
  return COMMON_MESSAGES.networkError
}
