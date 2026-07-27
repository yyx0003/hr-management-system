import { Outlet } from 'react-router-dom'
import { SideMenu } from '../components/SideMenu'

export function MainLayout() {
  return (
    <div className="main-layout">
      <SideMenu />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
