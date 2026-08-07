import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './lib/auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Home from './pages/Home'
import ServiceDetail from './pages/ServiceDetail'
import Orders from './pages/Orders'
import OrderDetail from './pages/OrderDetail'
import Merchant from './pages/Merchant'
import Admin from './pages/Admin'

function RequireAuth({ roles, children }: { roles?: string[]; children: JSX.Element }) {
  const { user, ready } = useAuth()
  const loc = useLocation()
  if (!ready) return <div className="min-h-screen bg-bg" />
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { user, ready } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={user && ready ? <Navigate to="/" replace /> : <Login />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Home />} />
        <Route path="/services/:id" element={<ServiceDetail />} />
        <Route path="/orders" element={<RequireAuth roles={['CONSUMER']}><Orders /></RequireAuth>} />
        <Route path="/orders/:id" element={<RequireAuth><OrderDetail /></RequireAuth>} />
        <Route
          path="/merchant"
          element={
            <RequireAuth roles={['MERCHANT', 'TECHNICIAN']}>
              <Merchant />
            </RequireAuth>
          }
        />
        <Route path="/admin" element={<RequireAuth roles={['ADMIN', 'SUPER_ADMIN']}><Admin /></RequireAuth>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
