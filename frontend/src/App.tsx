import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { PlaceholderPage } from './components/PlaceholderPage'
import { MenuPage } from './pages/MenuPage'
import { LoginPage } from './pages/LoginPage'
import { MainLayout } from './layouts/MainLayout'
import { ROUTES } from './constants/routes'
import { getAccessToken } from './features/auth/authStorage'
import './App.css'
import DepartmentPage from './pages/DepartmentPage'
import PositionPage from './pages/PositionPage'
import QualificationPage from './pages/QualificationPage'
import SkillGradePage from './pages/SkillGradePage'

function RootRedirect() {
  return <Navigate to={getAccessToken() ? ROUTES.menu : ROUTES.login} replace />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={ROUTES.login} element={<LoginPage />} />
        <Route path="/" element={<RootRedirect />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path={ROUTES.menu} element={<MenuPage />} />
            <Route path={ROUTES.employees} element={<PlaceholderPage title="社員管理" />} />
            <Route path={ROUTES.employeeNew} element={<PlaceholderPage title="社員登録" />} />
            <Route path={ROUTES.employeeDetail} element={<PlaceholderPage title="社員詳細" />} />
            <Route path={ROUTES.employeeEdit} element={<PlaceholderPage title="社員編集" />} />
            <Route path={ROUTES.attendances} element={<PlaceholderPage title="勤怠管理" />} />
            <Route path={ROUTES.attendanceImport} element={<PlaceholderPage title="勤怠インポート" />} />
            <Route path={ROUTES.csvExport} element={<PlaceholderPage title="CSV出力" />} />
            <Route path={ROUTES.department} element={<DepartmentPage/>} />
            <Route path={ROUTES.position} element={<PositionPage />} />
            <Route path={ROUTES.qualification} element={<QualificationPage />} />
            <Route path={ROUTES.skillgrade} element={<SkillGradePage />} />
          </Route>
        </Route>
        <Route path="*" element={<RootRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
