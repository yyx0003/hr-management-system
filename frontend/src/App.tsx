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
import { EmployeeListPage } from './features/employees/EmployeeListPage'
import { EmployeeDetailPage } from './features/employees/EmployeeDetailPage'
import { EmployeeCreatePage } from './features/employees/EmployeeCreatePage'
import { EmployeeEditPage } from './features/employees/EmployeeEditPage'
import { RetireeDeleteAdminPage } from './features/retiree/pages/RetireeDeleteAdminPage'
import './App.css'
import DepartmentPage from './pages/DepartmentPage'
import PositionPage from './pages/PositionPage'
import QualificationPage from './pages/QualificationPage'
import SkillGradePage from './pages/SkillGradePage'

function RootRedirect() {
  return (
    <Navigate
      to={getAccessToken() ? ROUTES.menu : ROUTES.login}
      replace
    />
  )
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

            <Route
              path={ROUTES.employees}
              element={<EmployeeListPage />}
            />
            <Route
              path={ROUTES.employeeNew}
              element={<EmployeeCreatePage />}
            />
            <Route
              path={ROUTES.employeeDetail}
              element={<EmployeeDetailPage />}
            />
            <Route
              path={ROUTES.employeeEdit}
              element={<EmployeeEditPage />}
            />

            <Route
              path={ROUTES.attendances}
              element={<AttendancePage />}
            />
            <Route path={ROUTES.retireeDelete} element={<RetireeDeleteAdminPage />} />
            <Route
              path={ROUTES.attendanceImport}
              element={<AttendanceImportPage />}
            />
            <Route
              path={ROUTES.csvExport}
              element={<CsvExportPage />}
            />

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
