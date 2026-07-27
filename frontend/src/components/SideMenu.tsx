import { NavLink, useNavigate } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { removeAuth } from '../features/auth/authStorage'

const menuItems = [
  { label: 'メニュー', to: ROUTES.menu },
  { label: '社員管理', to: ROUTES.employees },
  { label: '勤怠管理', to: ROUTES.attendances },
  { label: 'CSV出力', to: ROUTES.csvExport },
  { label: 'マスタデータ管理画面', to: ROUTES.department },
]

export function SideMenu() {
  const navigate = useNavigate()

  const handleLogout = () => {
    removeAuth()
    navigate(ROUTES.login, { replace: true })
  }

  return (
    <aside className="side-menu">
      <h1>人事管理システム</h1>
      <nav aria-label="メインメニュー">
        {menuItems.map((item) => (
          <NavLink key={item.to} to={item.to}>
            {item.label}
          </NavLink>
        ))}
        <button className="logout-button" type="button" onClick={handleLogout}>
          ログアウト
        </button>
      </nav>
    </aside>
  )
}
