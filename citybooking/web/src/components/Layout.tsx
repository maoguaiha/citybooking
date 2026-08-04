import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { MapPin, Package, Store, ShieldCheck, LogOut } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { IconButton } from '../lib/ui'

const ROLE_LABEL: Record<string, string> = {
  CONSUMER: '消费者',
  MERCHANT: '商家',
  TECHNICIAN: '技师',
  ADMIN: '管理员',
}

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()

  const links =
    user?.role === 'CONSUMER'
      ? [{ to: '/', label: '找服务', icon: MapPin, end: true }]
      : user?.role === 'MERCHANT' || user?.role === 'TECHNICIAN'
        ? [{ to: '/merchant', label: '工作台', icon: Store, end: true }]
        : [{ to: '/admin', label: '管理平台', icon: ShieldCheck, end: true }]

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-20 border-b border-line bg-bg/85 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-5">
          <NavLink to="/" className="flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-accent text-sm font-bold text-white">
              邻
            </span>
            <span className="text-lg font-semibold tracking-tight">邻约</span>
          </NavLink>

          <nav className="flex items-center gap-1">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) =>
                  `rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                    isActive ? 'bg-ink text-white' : 'text-muted hover:text-ink'
                  }`
                }
              >
                {l.label}
              </NavLink>
            ))}
            {user?.role === 'CONSUMER' && (
              <NavLink
                to="/orders"
                className={({ isActive }) =>
                  `flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                    isActive ? 'bg-ink text-white' : 'text-muted hover:text-ink'
                  }`
                }
              >
                <Package size={16} /> 我的订单
              </NavLink>
            )}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium leading-tight">{user?.nickname || user?.phone}</p>
              <p className="text-xs text-faint">{ROLE_LABEL[user?.role || '']}</p>
            </div>
            <IconButton
              title="退出登录"
              onClick={() => {
                logout()
                nav('/login')
              }}
            >
              <LogOut size={18} />
            </IconButton>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-5 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-col gap-1 px-5 py-6 text-xs text-faint sm:flex-row sm:items-center sm:justify-between">
          <span>邻约 · 同城上门服务预约平台</span>
          <span>模拟支付环境 · 仅用于演示</span>
        </div>
      </footer>
    </div>
  )
}
