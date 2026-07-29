import axios from 'axios'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { COMMON_MESSAGES } from '../../messages/commonMessages'
import { fetchEmployeeDetail } from './employeeApi'
import {
  fetchEffectiveDepartments,
  fetchEffectivePositions,
  fetchEffectiveSkillGrades,
  fetchQualificationNamesAtAcquisitionDates,
} from './employeeMasterApi'
import { today } from './employeeForm'
import './employeeForm.css'
import type { EmployeeDetail } from './types'

type SuccessState = { successMessage: string }

export function EmployeeDetailPage() {
  const { employeeNo } = useParams()
  const validEmployeeNo = employeeNo && /^\d{1,20}$/.test(employeeNo) ? employeeNo : null
  const navigate = useNavigate()
  const location = useLocation()
  const [employee, setEmployee] = useState<EmployeeDetail | null>(null)
  const [masterNames, setMasterNames] = useState<MasterNames>({ departments: new Map(), positions: new Map(), qualifications: new Map(), skillGrades: new Map() })
  const [masterLoadWarning, setMasterLoadWarning] = useState(false)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(() => Boolean(validEmployeeNo))
  const [successMessage] = useState<string | undefined>(() => isSuccessState(location.state) ? location.state.successMessage : undefined)

  useEffect(() => {
    if (isSuccessState(location.state)) navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  useEffect(() => {
    const detailEmployeeNo = validEmployeeNo ?? ''
    if (!detailEmployeeNo) return
    const controller = new AbortController()
    async function load() {
      setIsLoading(true)
      setError('')
      setMasterLoadWarning(false)
      try {
        const detail = await fetchEmployeeDetail(detailEmployeeNo, controller.signal)
        if (controller.signal.aborted) return
        setEmployee(detail)
        setIsLoading(false)
        void loadMasterNames(detail, controller.signal)
      } catch (loadError: unknown) {
        if (!controller.signal.aborted && !isCancelled(loadError)) setError(toDetailErrorMessage(loadError))
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    async function loadMasterNames(detail: EmployeeDetail, signal: AbortSignal) {
      const [departmentResult, positionResult, skillGradeResult, qualificationResult] = await Promise.allSettled([
        fetchEffectiveDepartments(today(), signal),
        fetchEffectivePositions(today(), signal),
        fetchEffectiveSkillGrades(today(), signal),
        fetchQualificationNamesAtAcquisitionDates(detail.qualifications.map((qualification) => qualification.acquisitionDate), signal),
      ])
      if (signal.aborted) return

      const nextMasterNames: MasterNames = { departments: new Map(), positions: new Map(), qualifications: new Map(), skillGrades: new Map() }
      let hasFailure: boolean
      if (departmentResult.status === 'fulfilled') {
        nextMasterNames.departments = new Map(departmentResult.value.map((item) => [item.departmentId, item.departmentName]))
        hasFailure = !nextMasterNames.departments.has(detail.departmentId)
      } else hasFailure = true
      if (positionResult.status === 'fulfilled') {
        nextMasterNames.positions = new Map(positionResult.value.map((item) => [item.positionId, item.positionName]))
        hasFailure = hasFailure || (detail.positionId !== null && !nextMasterNames.positions.has(detail.positionId))
      } else hasFailure = true
      if (skillGradeResult.status === 'fulfilled') {
        nextMasterNames.skillGrades = new Map(skillGradeResult.value.map((item) => [item.skillGrade, `${item.skillGrade}等級`]))
        hasFailure = hasFailure || !nextMasterNames.skillGrades.has(detail.skillGrade)
      } else hasFailure = true
      if (qualificationResult.status === 'fulfilled') {
        hasFailure = hasFailure || qualificationResult.value.hasFailure
        detail.qualifications.forEach((qualification) => {
          const qualificationName = qualificationResult.value.names.get(qualification.acquisitionDate)?.get(qualification.qualificationId)
          if (qualificationName) nextMasterNames.qualifications.set(qualification.qualificationId, qualificationName)
          else hasFailure = true
        })
      }
      if (signal.aborted) return
      setMasterNames(nextMasterNames)
      setMasterLoadWarning(hasFailure)
    }
    void load()
    return () => controller.abort()
  }, [validEmployeeNo])

  return <section className="employee-detail-page" aria-labelledby="employee-detail-title">
    <h2 id="employee-detail-title" className="employee-list-title">社員詳細</h2>
    {successMessage && <p className="employee-success-message" role="status">{successMessage}</p>}
    {!validEmployeeNo ? <p className="employee-api-message" role="alert">社員番号の形式が正しくありません。</p>
      : isLoading ? <p className="employee-loading-message" role="status">社員情報を読み込み中です...</p>
      : error ? <p className="employee-api-message" role="alert">{error}</p>
        : employee && <>
          {masterLoadWarning && <p className="employee-api-message" role="status">一部の名称を取得できませんでした。</p>}
          <section className="employee-detail-card" aria-label="社員基本情報">
            <section className="employee-detail-section" aria-labelledby="employee-basic-info-title">
              <h3 id="employee-basic-info-title" className="employee-card-title">基本情報</h3>
              <dl className="employee-detail-grid">
              <Detail label="社員番号" value={employee.employeeNo} />
              <Detail label="氏名" value={employee.employeeName} />
              <Detail label="生年月日" value={employee.birthDate} />
              <Detail label="郵便番号" value={employee.postalCode} />
              <Detail fullWidth label="住所" value={employee.address} />
              <Detail label="電話番号" value={employee.phoneNumber ?? '未設定'} />
              <Detail label="メールアドレス" value={employee.emailAddress ?? '未設定'} />
              <Detail label="入社日" value={employee.hireDate} />
              <Detail label="退職日" value={employee.retireDate ?? '未設定'} />
              </dl>
            </section>
            <section className="employee-detail-section" aria-labelledby="employee-assignment-info-title">
              <h3 id="employee-assignment-info-title" className="employee-card-title">所属情報</h3>
              <dl className="employee-detail-grid">
              <Detail label="所属部署" value={masterNames.departments.get(employee.departmentId) ?? `部署ID: ${employee.departmentId}`} />
              <Detail label="役職" value={employee.positionId === null ? '役職なし' : (masterNames.positions.get(employee.positionId) ?? `役職ID: ${employee.positionId}`)} />
              <Detail label="職能資格" value={masterNames.skillGrades.get(employee.skillGrade) ?? `${employee.skillGrade}級`} />
            </dl>
            </section>
          </section>
          <section className="employee-detail-card" aria-labelledby="employee-held-qualifications-title">
            <h3 id="employee-held-qualifications-title" className="employee-card-title">保有資格</h3>
            {employee.qualifications.length === 0 ? <p className="employee-no-qualifications">保有資格はありません。</p> : <ul className="employee-held-qualifications">
              {employee.qualifications.map((qualification) => <li key={qualification.qualificationId}>
                {masterNames.qualifications.get(qualification.qualificationId) ?? `資格ID: ${qualification.qualificationId}`}（取得日: {qualification.acquisitionDate}）
              </li>)}
            </ul>}
          </section>
        </>}
    <div className="employee-list-actions">
      <button className="employee-secondary-button" type="button" onClick={() => navigate(ROUTES.employees)}>一覧へ戻る</button>
      <button className="employee-primary-button" disabled={!employee || Boolean(error)} type="button" onClick={() => employeeNo && navigate(ROUTES.employeeEdit.replace(':employeeNo', encodeURIComponent(employeeNo)))}>更新画面へ</button>
    </div>
  </section>
}

type MasterNames = {
  departments: Map<number, string>
  positions: Map<number, string>
  qualifications: Map<number, string>
  skillGrades: Map<number, string>
}

function Detail({ label, value, fullWidth = false }: { label: string; value: string; fullWidth?: boolean }) {
  return <div className={fullWidth ? 'employee-detail-item-full-width' : undefined}><dt>{label}</dt><dd>{value}</dd></div>
}

function isSuccessState(value: unknown): value is SuccessState {
  return Boolean(value) && typeof value === 'object' && typeof (value as { successMessage?: unknown }).successMessage === 'string'
}

function isCancelled(error: unknown) {
  return axios.isCancel(error) || (axios.isAxiosError(error) && error.code === 'ERR_CANCELED')
}

function toDetailErrorMessage(error: unknown) {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    if (error.response?.status === 404) return error.response.data?.message ?? '社員情報が見つかりません。'
    if (error.response && error.response.status >= 500) return COMMON_MESSAGES.serverError
    if (error.response?.status === 400) return error.response.data?.message ?? COMMON_MESSAGES.networkError
  }
  return COMMON_MESSAGES.networkError
}
