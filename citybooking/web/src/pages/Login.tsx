import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MapPin, User, Store, Wrench, ShieldCheck, ArrowRight } from 'lucide-react'
import { useAuth, type Role } from '../lib/auth'
import { Button, Field, Input } from '../lib/ui'

const ROLES: { value: Role; label: string; icon: typeof User; desc: string }[] = [
  { value: 'CONSUMER', label: '我是消费者', icon: User, desc: '预约上门服务' },
  { value: 'MERCHANT', label: '我是商家', icon: Store, desc: '入驻接单经营' },
  { value: 'TECHNICIAN', label: '我是独立技师', icon: Wrench, desc: '凭手艺接单' },
  { value: 'ADMIN', label: '平台管理员', icon: ShieldCheck, desc: '审核与运营' },
]

const DEMO = { phone: '13800000001', password: 'demo1234' }

export default function Login() {
  const { login, register } = useAuth()
  const nav = useNavigate()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [role, setRole] = useState<Role>('CONSUMER')
  const [phone, setPhone] = useState(DEMO.phone)
  const [password, setPassword] = useState(DEMO.password)
  const [nickname, setNickname] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    setBusy(true)
    try {
      if (mode === 'login') {
        await login(phone, password)
      } else {
        if (!nickname.trim()) throw new Error('请填写昵称')
        await register(phone, password, nickname.trim(), role)
      }
      nav(role === 'CONSUMER' ? '/' : role === 'ADMIN' ? '/admin' : '/merchant')
    } catch (e2) {
      setErr((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* 品牌侧 */}
      <aside className="relative hidden overflow-hidden bg-ink lg:block">
        <img
          src="https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=1400&q=80"
          alt=""
          className="absolute inset-0 h-full w-full object-cover opacity-60"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/70 to-ink/30" />
        <div className="relative flex h-full flex-col justify-between p-12 text-white">
          <div className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-accent text-base font-bold">邻</span>
            <span className="text-xl font-semibold">邻约</span>
          </div>
          <div>
            <h1 className="max-w-md text-4xl font-semibold leading-tight tracking-tight">
              附近的好服务，<br />一键预约上门。
            </h1>
            <p className="mt-4 max-w-md text-white/70">
              家政、维修、美容、陪诊……多品类上门服务，附近认证师傅抢单或指定，明码标价，全程可追溯。
            </p>
            <div className="mt-8 flex items-center gap-2 text-sm text-white/80">
              <MapPin size={16} /> 已覆盖 120+ 城市 · 实时定位附近师傅
            </div>
          </div>
        </div>
      </aside>

      {/* 表单侧 */}
      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm animate-fade-in">
          <div className="mb-8 lg:hidden">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-accent text-base font-bold text-white">邻</span>
          </div>
          <h2 className="text-2xl font-semibold tracking-tight">
            {mode === 'login' ? '登录邻约' : '注册新账号'}
          </h2>
          <p className="mt-1 text-sm text-muted">
            {mode === 'login' ? '欢迎回来，继续你的预约。' : '选择身份，开启你的邻约之旅。'}
          </p>

          <div className="mt-6 grid grid-cols-2 gap-2">
            {ROLES.map((r) => {
              const Icon = r.icon
              const active = role === r.value
              return (
                <button
                  type="button"
                  key={r.value}
                  onClick={() => setRole(r.value)}
                  className={`flex items-start gap-2 rounded-xl border p-3 text-left transition ${
                    active ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'
                  }`}
                >
                  <Icon size={18} className={active ? 'text-accent' : 'text-muted'} />
                  <span>
                    <span className="block text-sm font-medium text-ink">{r.label}</span>
                    <span className="block text-xs text-faint">{r.desc}</span>
                  </span>
                </button>
              )
            })}
          </div>

          <form className="mt-6 space-y-4" onSubmit={submit}>
            <Field label="手机号">
              <Input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="11 位手机号" inputMode="tel" />
            </Field>
            <Field label="密码">
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="登录密码"
              />
            </Field>
            {mode === 'register' && (
              <Field label="昵称">
                <Input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="展示名称" />
              </Field>
            )}

            {err && <p className="text-sm text-bad">{err}</p>}

            <Button type="submit" size="lg" className="w-full" disabled={busy}>
              {busy ? '处理中…' : mode === 'login' ? '登录' : '注册并进入'}
              {!busy && <ArrowRight size={18} />}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted">
            {mode === 'login' ? '还没有账号？' : '已有账号？'}
            <button
              className="ml-1 font-medium text-accent hover:underline"
              onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
            >
              {mode === 'login' ? '立即注册' : '去登录'}
            </button>
          </p>
          <p className="mt-3 text-center text-xs text-faint">演示账户：{DEMO.phone} / {DEMO.password}</p>
        </div>
      </section>
    </div>
  )
}
