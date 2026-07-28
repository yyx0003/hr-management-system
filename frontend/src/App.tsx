import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { PlaceholderPage } from './components/PlaceholderPage'
import { MenuPage } from './pages/MenuPage'
import { LoginPage } from './pages/LoginPage'
import { MainLayout } from './layouts/MainLayout'
import { ROUTES } from './constants/routes'
import { getAccessToken } from './features/auth/authStorage'
import { AttendancePage } from './features/attendance/pages/AttendancePage'
import { AttendanceImportPage } from './features/attendance/pages/AttendanceImportPage'
import { CsvExportPage } from './features/attendance/pages/CsvExportPage'
import './App.css'

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
            <Route path={ROUTES.attendances} element={<AttendancePage />}/>
            <Route path={ROUTES.attendanceImport} element={<AttendanceImportPage />}/>
            <Route path={ROUTES.csvExport} element={<CsvExportPage />}/>
            <Route path={ROUTES.department} element={<PlaceholderPage title="部門マスタ" />} />
            <Route path={ROUTES.position} element={<PlaceholderPage title="役職マスタ" />} />
            <Route path={ROUTES.qualification} element={<PlaceholderPage title="資格マスタ" />} />
            <Route path={ROUTES.skillgrade} element={<PlaceholderPage title="スキル等級マスタ" />} />
          </Route>
        </Route>
        <Route path="*" element={<RootRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
