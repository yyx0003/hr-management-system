import { useState } from 'react'
import { triggerRetireeDelete } from '../../../api/batch'
import './retireeDelete.css'

export function RetireeDeleteAdminPage() {
  const [isRunning, setIsRunning] = useState(false)
  const [result, setResult] = useState<{ status: string; message: string } | null>(null)
  const [error, setError] = useState('')

  const handleRun = async () => {
    setIsRunning(true)
    setResult(null)
    setError('')
    try {
      const data = await triggerRetireeDelete()
      setResult(data)
    } catch {
      setError('実行に失敗しました。')
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <div className="retiree-page">
      <div className="retiree-page-header">
        <div>
          <p className="retiree-page-eyebrow">BATCH</p>
          <h2>退職者削除バッチ</h2>
          <p className="retiree-page-description">
            退職後3か月以上経過した社員のデータを削除します。
          </p>
        </div>
      </div>

      <section className="retiree-card">
        <div className="retiree-info-box">
          <span className="retiree-info-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M12 8v4.5" />
              <path d="M12 16.5h.01" />
              <circle cx="12" cy="12" r="9" />
            </svg>
          </span>
          <p>
            通常は毎日3:00に自動実行されます。このページからは手動で実行できます。
          </p>
        </div>

        <button
          className="retiree-primary-button"
          disabled={isRunning}
          type="button"
          onClick={handleRun}
        >
          {isRunning ? '実行中...' : 'バッチを実行'}
        </button>

        {result && (
          <div className="retiree-result retiree-result-success" role="status">
            <strong>{result.status}</strong>
            <p>{result.message}</p>
          </div>
        )}

        {error && (
          <div className="retiree-result retiree-result-error" role="alert">
            {error}
          </div>
        )}
      </section>
    </div>
  )
}