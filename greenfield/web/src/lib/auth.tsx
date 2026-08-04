import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import * as api from './api'

export type Role = 'CONSUMER' | 'MERCHANT' | 'TECHNICIAN' | 'ADMIN'

interface AuthState {
  token: string | null
  user: api.UserInfo | null
  ready: boolean
  login: (phone: string, password: string) => Promise<void>
  register: (phone: string, password: string, nickname: string, role: Role) => Promise<void>
  logout: () => void
  refresh: () => Promise<void>
}

const Ctx = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('cb_token'))
  const [user, setUser] = useState<api.UserInfo | null>(null)
  const [ready, setReady] = useState(false)

  const persist = (t: string, u: api.UserInfo) => {
    localStorage.setItem('cb_token', t)
    localStorage.setItem('cb_user', JSON.stringify(u))
    setToken(t)
    setUser(u)
  }

  const refresh = async () => {
    if (!localStorage.getItem('cb_token')) {
      setReady(true)
      return
    }
    try {
      const u = await api.me()
      persist(localStorage.getItem('cb_token')!, u)
    } catch {
      localStorage.removeItem('cb_token')
      localStorage.removeItem('cb_user')
      setToken(null)
      setUser(null)
    } finally {
      setReady(true)
    }
  }

  useEffect(() => {
    refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (phone: string, password: string) => {
    const r = await api.login(phone, password)
    const u = await api.me().catch(() => ({ id: r.userId, phone, nickname: phone, role: r.role, status: 1 }))
    persist(r.token, u)
  }

  const register = async (phone: string, password: string, nickname: string, role: Role) => {
    const r = await api.register(phone, password, nickname, role)
    const u: api.UserInfo = { id: r.userId, phone, nickname, role: r.role, status: 1 }
    persist(r.token, u)
  }

  const logout = () => {
    localStorage.removeItem('cb_token')
    localStorage.removeItem('cb_user')
    setToken(null)
    setUser(null)
  }

  return (
    <Ctx.Provider value={{ token, user, ready, login, register, logout, refresh }}>
      {children}
    </Ctx.Provider>
  )
}

export function useAuth() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useAuth must be used within AuthProvider')
  return v
}

export const homeForRole = (role?: string): string => {
  switch (role) {
    case 'MERCHANT':
    case 'TECHNICIAN':
      return '/merchant'
    case 'ADMIN':
      return '/admin'
    default:
      return '/'
  }
}
