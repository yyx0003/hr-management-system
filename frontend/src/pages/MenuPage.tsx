import { Link } from 'react-router-dom'
import { ROUTES } from '../constants/routes'

const featureLinks = [
  { label: '社員管理', to: ROUTES.employees },
  { label: '勤怠管理', to: ROUTES.attendances },
  { label: 'CSV出力', to: ROUTES.csvExport },
  { label: 'マスタデータ管理画面', to: ROUTES.department },
  { label: '退職者削除', to: ROUTES.retireeDelete },
]

export function MenuPage() {
  return (
    <section>
      <h2 className="page-title">メニュー</h2>
      <p>利用する機能を選択してください。</p>
      <div className="menu-links">
        {featureLinks.map((feature) => (
          <Link key={feature.to} to={feature.to}>
            {feature.label}
          </Link>
        ))}
      </div>
    </section>
  )
}
