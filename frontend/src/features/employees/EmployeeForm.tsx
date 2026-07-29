import { cloneElement, type FormEvent, type ReactElement, useEffect, useRef, useState } from 'react'
import type { Department, Position, Qualification, SkillGrade } from '../master/types'
import { hasEmployeeFormErrors, validateEmployeeForm } from './employeeForm'
import type { EmployeeFormValues } from './types'
import './employeeForm.css'

type EmployeeFormProps = {
  initialValues: EmployeeFormValues
  departments: Department[]
  positions: Position[]
  qualifications: Qualification[]
  skillGrades: SkillGrade[]
  requiresHireDate: boolean
  isSubmitting?: boolean
  submitLabel: string
  submitError?: string
  onSubmit: (values: EmployeeFormValues) => Promise<void>
  onCancel?: () => void
}

export function EmployeeForm({
  initialValues,
  departments,
  positions,
  qualifications,
  skillGrades,
  requiresHireDate,
  isSubmitting = false,
  submitLabel,
  submitError,
  onSubmit,
  onCancel,
}: EmployeeFormProps) {
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState(() => validateEmployeeForm(initialValues, requiresHireDate))
  const [localSubmitting, setLocalSubmitting] = useState(false)
  const isMountedRef = useRef(true)
  const submitting = isSubmitting || localSubmitting

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  const update = <K extends Exclude<keyof EmployeeFormValues, 'qualifications'>>(key: K, value: EmployeeFormValues[K]) => {
    const next = { ...values, [key]: value }
    setValues(next)
    setErrors(validateEmployeeForm(next, requiresHireDate))
  }

  const updateQualification = (index: number, key: 'qualificationId' | 'acquisitionDate', value: string) => {
    const next = {
      ...values,
      qualifications: values.qualifications.map((qualification, qualificationIndex) => (
        qualificationIndex === index ? { ...qualification, [key]: value } : qualification
      )),
    }
    setValues(next)
    setErrors(validateEmployeeForm(next, requiresHireDate))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (submitting) return
    const nextErrors = validateEmployeeForm(values, requiresHireDate)
    setErrors(nextErrors)
    if (hasEmployeeFormErrors(nextErrors)) return
    setLocalSubmitting(true)
    try {
      await onSubmit(values)
    } finally {
      if (isMountedRef.current) setLocalSubmitting(false)
    }
  }

  return (
    <form className="employee-form" noValidate onSubmit={handleSubmit}>
      {submitError && <p className="employee-api-message" role="alert">{submitError}</p>}
      <section className="employee-form-card" aria-labelledby="employee-basic-info-title">
        <h3 id="employee-basic-info-title" className="employee-card-title">基本情報</h3>
        <div className="employee-form-grid">
          <Field id="employee-name" label="氏名" error={errors.employeeName}>
            <input value={values.employeeName} maxLength={100} onChange={(event) => update('employeeName', event.target.value)} />
          </Field>
          <Field id="employee-birth-date" label="生年月日" error={errors.birthDate}>
            <input type="date" value={values.birthDate} onChange={(event) => update('birthDate', event.target.value)} />
          </Field>
          <Field id="employee-postal-code" label="郵便番号" error={errors.postalCode}>
            <input inputMode="numeric" maxLength={7} value={values.postalCode} onChange={(event) => update('postalCode', event.target.value)} />
          </Field>
          <Field id="employee-address" label="住所" error={errors.address}>
            <input maxLength={255} value={values.address} onChange={(event) => update('address', event.target.value)} />
          </Field>
          <Field id="employee-phone-number" label="電話番号" error={errors.phoneNumber}>
            <input inputMode="numeric" maxLength={20} value={values.phoneNumber} onChange={(event) => update('phoneNumber', event.target.value)} />
          </Field>
          <Field id="employee-email-address" label="メールアドレス" error={errors.emailAddress}>
            <input type="email" maxLength={255} value={values.emailAddress} onChange={(event) => update('emailAddress', event.target.value)} />
          </Field>
          {requiresHireDate && <Field id="employee-hire-date" label="入社日" error={errors.hireDate}>
            <input type="date" value={values.hireDate} onChange={(event) => update('hireDate', event.target.value)} />
          </Field>}
          <Field id="employee-retire-date" label="退職日" error={errors.retireDate}>
            <input type="date" value={values.retireDate} onChange={(event) => update('retireDate', event.target.value)} />
          </Field>
        </div>
      </section>

      <section className="employee-form-card" aria-labelledby="employee-assignment-title">
        <h3 id="employee-assignment-title" className="employee-card-title">所属情報</h3>
        <div className="employee-form-grid">
          <Field id="employee-department-id" label="所属部署" error={errors.departmentId}>
            <select value={values.departmentId} onChange={(event) => update('departmentId', event.target.value)}>
              <option value="">選択してください</option>
              {departments.map((item) => <option key={item.departmentId} value={item.departmentId}>{item.departmentName}</option>)}
            </select>
          </Field>
          <Field id="employee-position-id" label="役職">
            <select value={values.positionId} onChange={(event) => update('positionId', event.target.value)}>
              <option value="">役職なし</option>
              {positions.map((item) => <option key={item.positionId} value={item.positionId}>{item.positionName}</option>)}
            </select>
          </Field>
          <Field id="employee-skill-grade" label="職能資格" error={errors.skillGrade}>
            <select value={values.skillGrade} onChange={(event) => update('skillGrade', event.target.value)}>
              <option value="">選択してください</option>
              {skillGrades.map((item) => <option key={item.skillGrade} value={item.skillGrade}>{item.skillGrade}級</option>)}
            </select>
          </Field>
        </div>
      </section>

      <section className="employee-form-card" aria-labelledby="employee-qualifications-title">
        <div className="employee-form-section-heading">
          <h3 id="employee-qualifications-title" className="employee-card-title">保有資格</h3>
          <button className="employee-secondary-button" type="button" onClick={() => {
            const next = { ...values, qualifications: [...values.qualifications, { qualificationId: '', acquisitionDate: '' }] }
            setValues(next)
            setErrors(validateEmployeeForm(next, requiresHireDate))
          }} disabled={submitting}>資格を追加</button>
        </div>
        <div className="employee-qualification-list">
          {values.qualifications.map((qualification, index) => (
            <div className="employee-qualification-row" key={`${index}-${qualification.qualificationId}`}>
              <Field id={`employee-qualification-id-${index}`} label="資格" error={errors.qualifications[index].qualificationId}>
                <select value={qualification.qualificationId} onChange={(event) => updateQualification(index, 'qualificationId', event.target.value)}>
                  <option value="">選択してください</option>
                  {qualifications.map((item) => <option key={item.qualificationId} value={item.qualificationId}>{item.qualificationName}</option>)}
                </select>
              </Field>
              <Field id={`employee-qualification-acquisition-date-${index}`} label="取得日" error={errors.qualifications[index].acquisitionDate}>
                <input type="date" value={qualification.acquisitionDate} onChange={(event) => updateQualification(index, 'acquisitionDate', event.target.value)} />
              </Field>
              <button className="employee-secondary-button employee-qualification-remove" type="button" onClick={() => {
                const next = { ...values, qualifications: values.qualifications.filter((_, itemIndex) => itemIndex !== index) }
                setValues(next)
                setErrors(validateEmployeeForm(next, requiresHireDate))
              }} disabled={submitting}>削除</button>
            </div>
          ))}
        </div>
      </section>
      <div className="employee-form-actions">
        <button className="employee-primary-button" disabled={submitting} type="submit">{submitting ? '送信中...' : submitLabel}</button>
        {onCancel && <button className="employee-secondary-button" disabled={submitting} type="button" onClick={onCancel}>戻る</button>}
      </div>
    </form>
  )
}

type FieldControlProps = {
  id?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

function Field({ id, label, error, children }: { id: string; label: string; error?: string; children: ReactElement<FieldControlProps> }) {
  const errorId = `${id}-error`
  return <div className="employee-form-field">
    <label htmlFor={id}>{label}</label>
    {cloneElement(children, {
      id,
      'aria-invalid': error ? true : undefined,
      'aria-describedby': error ? errorId : undefined,
    })}
    {error && <span className="employee-field-error" id={errorId} role="alert">{error}</span>}
  </div>
}
