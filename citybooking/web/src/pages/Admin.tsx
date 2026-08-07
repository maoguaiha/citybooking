import { useEffect, useState } from 'react'
import { ShieldCheck, Store, Tags, Receipt, HandCoins, CheckCircle2, XCircle, UserCog, LayoutDashboard, Wrench, Ban, RotateCcw, Package, Users as UsersIcon } from 'lucide-react'
import * as api from '../lib/api'
import { useAuth } from '../lib/auth'
import { Button, Card, EmptyState, Field, Input, Money, Spinner, StatusBadge } from '../lib/ui'
import { fmtDateTime } from '../lib/format'

type Tab = 'dashboard' | 'merchants' | 'categories' | 'orders' | 'refunds' | 'admins' | 'users' | 'technicians' | 'services'

const MERCHANT_STATUS: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

export default function Admin() {
  const [tab, setTab] = useState<Tab>('merchants')
  const { user } = useAuth()

  return (
    <div className="animate-fade-in">
      <h1 className="text-xl font-semibold">平台管理</h1>
      <p className="mt-1 text-sm text-muted">商家审核、分类与订单运营、退款仲裁。</p>

      <div className="mt-5 flex flex-wrap gap-1 border-b border-line">
        {buildTabs(user?.role).map(([k, label, Icon]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`flex items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition ${
              tab === k ? 'border-accent text-ink' : 'border-transparent text-muted hover:text-ink'
            }`}
          >
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {tab === 'dashboard' && <Dashboard />}
        {tab === 'merchants' && <Merchants />}
        {tab === 'categories' && <Categories />}
        {tab === 'orders' && <Orders />}
        {tab === 'refunds' && <Refunds />}
        {tab === 'admins' && <Admins />}
        {tab === 'users' && <Users />}
        {tab === 'technicians' && <Techs />}
        {tab === 'services' && <Services />}
      </div>
    </div>
  )
}

function Merchants() {
  const [list, setList] = useState<api.MerchantView[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [reasons, setReasons] = useState<Record<number, string>>({})
  const load = () => {
    setLoading(true)
    api.adminMerchants('PENDING').then(setList).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const act = async (id: number, approve: boolean, reason?: string) => {
    setBusy(true)
    try {
      await api.adminAudit(id, approve, approve ? undefined : (reason || undefined))
      setReasons((r) => { const n = { ...r }; delete n[id]; return n })
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载待审商家…" />
  if (list.length === 0) return <EmptyState icon={<ShieldCheck size={28} />} title="没有待审核的商家" />

  return (
    <div className="space-y-4">
      {list.map((m) => (
        <Card key={m.id} className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="font-medium text-ink">{m.name}</h3>
              <p className="mt-1 text-sm text-muted">ID {m.id} · {m.address || '未填写地址'}</p>
              <p className="mt-1 text-xs text-faint">状态：{MERCHANT_STATUS[m.status] || m.status}</p>
              {m.rejectReason && (
                <p className="mt-1 text-xs text-bad">拒绝原因：{m.rejectReason}</p>
              )}
            </div>
            <div className="flex flex-col items-end gap-2">
              <div className="flex gap-2">
                <Button size="sm" disabled={busy} onClick={() => act(m.id, true)}>
                  <CheckCircle2 size={15} /> 通过
                </Button>
                <Button
                  size="sm"
                  variant="danger"
                  disabled={busy || !reasons[m.id]?.trim()}
                  onClick={() => act(m.id, false, reasons[m.id])}
                >
                  <XCircle size={15} /> 拒绝
                </Button>
              </div>
              <Input
                value={reasons[m.id] ?? ''}
                onChange={(e) => setReasons({ ...reasons, [m.id]: e.target.value })}
                placeholder="拒绝原因（拒绝时必填）"
                className="w-56"
              />
            </div>
          </div>
        </Card>
      ))}
    </div>
  )
}

function Categories() {
  const [list, setList] = useState<api.Category[]>([])
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api.categories().then(setList).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const add = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    setBusy(true)
    try {
      await api.createCategory(name.trim())
      setName('')
      load()
    } catch (e2) {
      alert((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载分类…" />
  return (
    <div className="grid gap-6 md:grid-cols-2">
      <Card className="p-5">
        <h3 className="font-medium">分类列表</h3>
        <ul className="mt-3 divide-y divide-line">
          {list.map((c) => (
            <li key={c.id} className="flex items-center justify-between py-2.5 text-sm">
              <span>{c.name}</span>
              <span className="text-faint">ID {c.id}</span>
            </li>
          ))}
          {list.length === 0 && <li className="py-3 text-sm text-muted">暂无分类</li>}
        </ul>
      </Card>
      <Card className="p-5">
        <h3 className="font-medium">新增分类</h3>
        <form className="mt-3 space-y-3" onSubmit={add}>
          <Field label="分类名称">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="如：宠物服务" />
          </Field>
          <Button type="submit" disabled={busy}>
            {busy ? '添加中…' : '添加'}
          </Button>
        </form>
      </Card>
    </div>
  )
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 text-sm">
      <span className="text-muted">{label}</span>
      <span className="text-right">{value}</span>
    </div>
  )
}

function Orders() {
  const [data, setData] = useState<api.PageResult<api.OrderView> | null>(null)
  const [loading, setLoading] = useState(true)
  const [kw, setKw] = useState('')
  const [status, setStatus] = useState<string | undefined>()
  const [page, setPage] = useState(1)
  const [detail, setDetail] = useState<api.OrderView | null>(null)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api.adminOrders({ page, size: 10, keyword: kw || undefined, status })
      .then(setData).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [page, kw, status])

  const open = async (id: number) => {
    setBusy(true)
    try { setDetail(await api.adminOrderDetail(id)) } catch (e) { alert((e as Error).message) } finally { setBusy(false) }
  }

  if (loading && !data) return <Spinner label="加载订单…" />
  const total = data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / 10))
  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <Input value={kw} onChange={(e) => { setKw(e.target.value); setPage(1) }} placeholder="搜索订单号" />
        <Button onClick={() => setPage(1)}>搜索</Button>
        {[undefined, 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED', 'SERVICING', 'COMPLETED', 'CANCELLED', 'REFUNDED'].map((s) => (
          <button
            key={s ?? 'all'}
            onClick={() => { setStatus(s); setPage(1) }}
            className={`rounded-full px-3 py-1.5 text-sm transition ${
              status === s ? 'bg-ink text-white' : 'bg-line/60 text-muted hover:text-ink'
            }`}
          >
            {s ?? '全部'}
          </button>
        ))}
      </div>
      {(data?.records ?? []).length === 0 ? (
        <EmptyState icon={<Receipt size={28} />} title="没有符合条件的订单" />
      ) : (
        <div className="space-y-3">
          {(data?.records ?? []).map((o) => (
            <Card key={o.id} className="p-4 cursor-pointer hover:shadow" onClick={() => open(o.id)}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="font-medium text-ink">{o.serviceTitle || '上门服务'}</span>
                  <span className="ml-2 text-xs text-faint">{o.orderNo}</span>
                  <p className="mt-0.5 text-xs text-muted">
                    下单 {fmtDateTime(o.createdAt)} · 方式 {o.mode === 'APPOINT' ? '指定' : '抢单'}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Money value={o.amount} className="font-semibold text-ink" />
                  <StatusBadge status={o.status} />
                  <Button size="sm" variant="ghost" onClick={(e) => { e.stopPropagation(); open(o.id) }}>详情</Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
      <div className="mt-4 flex items-center justify-between text-sm text-muted">
        <span>共 {total} 条</span>
        <div className="space-x-2">
          <Button size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</Button>
          <span>第 {page} / {pages} 页</span>
          <Button size="sm" disabled={page >= pages} onClick={() => setPage((p) => p + 1)}>下一页</Button>
        </div>
      </div>
      {detail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setDetail(null)}>
          <Card className="w-[440px] max-w-[92vw] space-y-2" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">订单详情 #{detail.id}</h3>
              <button className="text-muted" onClick={() => setDetail(null)}>✕</button>
            </div>
            <DetailRow label="订单号" value={<span className="font-mono text-xs">{detail.orderNo}</span>} />
            <DetailRow label="消费者" value={String(detail.consumerId)} />
            <DetailRow label="商家" value={String(detail.merchantId ?? '-')} />
            <DetailRow label="技师" value={String(detail.technicianId ?? '-')} />
            <DetailRow label="服务" value={String(detail.serviceId ?? '-')} />
            <DetailRow label="金额" value={<Money value={detail.amount} />} />
            <DetailRow label="状态" value={<StatusBadge status={detail.status} />} />
            <DetailRow label="支付状态" value={detail.payStatus} />
            <DetailRow label="退款状态" value={detail.refundStatus} />
            <DetailRow label="预约时间" value={detail.appointmentTime ? String(detail.appointmentTime) : '-'} />
          </Card>
        </div>
      )}
    </div>
  )
}

function Refunds() {
  const [list, setList] = useState<api.OrderView[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const load = () => {
    setLoading(true)
    api.adminOrders({ status: 'REFUNDED', page: 1, size: 50 }).then((d) => setList(d.records)).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const approve = async (id: number) => {
    setBusy(true)
    try {
      await api.adminRefundApprove(id)
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const reject = async (id: number) => {
    if (!confirm(`确认拒绝订单 #${id} 的退款申请？`)) return
    setBusy(true)
    try {
      await api.adminRefundReject(id)
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载退款申请…" />
  if (list.length === 0) return <EmptyState icon={<HandCoins size={28} />} title="暂无待仲裁的退款" />

  return (
    <div className="space-y-3">
      {list.map((o) => (
        <Card key={o.id} className="p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <span className="font-medium text-ink">{o.serviceTitle || '上门服务'}</span>
              <p className="mt-0.5 text-xs text-muted">
                {o.orderNo} · 退款状态 {o.refundStatus}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <Money value={o.amount} className="font-semibold text-ink" />
              <Button size="sm" disabled={busy} onClick={() => approve(o.id)}>
                <CheckCircle2 size={15} /> 同意退款
              </Button>
              <Button size="sm" variant="danger" disabled={busy} onClick={() => reject(o.id)}>
                <XCircle size={15} /> 拒绝
              </Button>
            </div>
          </div>
        </Card>
      ))}
    </div>
  )
}

function Dashboard() {
  const [d, setD] = useState<api.DashboardView | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    setLoading(true)
    api.adminDashboard().then(setD).catch(() => {}).finally(() => setLoading(false))
  }, [])
  if (loading) return <Spinner label="加载看板…" />
  if (!d) return <EmptyState icon={<LayoutDashboard size={28} />} title="暂无数据" />
  const cards = [
    { label: '今日订单', value: d.todayOrderCount, money: false },
    { label: '今日 GMV', value: d.todayGmv, money: true },
    { label: '消费者总数', value: d.totalUsers, money: false },
    { label: '商户总数', value: d.totalMerchants, money: false },
    { label: '待审商户', value: d.pendingMerchants, money: false },
    { label: '技师总数', value: d.totalTechnicians, money: false },
    { label: '待处理退款', value: d.pendingRefunds, money: false },
    { label: '服务总数', value: d.totalServices, money: false },
  ]
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((c) => (
        <Card key={c.label} className="p-5">
          <p className="text-sm text-muted">{c.label}</p>
          <div className="mt-2">
            {c.money ? (
              <Money value={c.value} className="text-2xl font-semibold text-ink" />
            ) : (
              <span className="text-2xl font-semibold text-ink">{c.value}</span>
            )}
          </div>
        </Card>
      ))}
    </div>
  )
}

function Users() {
  const [data, setData] = useState<api.PageResult<api.UserView> | null>(null)
  const [loading, setLoading] = useState(true)
  const [kw, setKw] = useState('')
  const [page, setPage] = useState(1)
  const [ordersOf, setOrdersOf] = useState<api.UserView | null>(null)
  const [orders, setOrders] = useState<api.PageResult<api.OrderView> | null>(null)

  const load = () => {
    setLoading(true)
    api.adminUsers({ page, size: 10, keyword: kw || undefined })
      .then(setData).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [page, kw])

  const toggleBan = async (u: api.UserView, ban: boolean) => {
    await (ban ? api.banUser(u.id) : api.unbanUser(u.id))
    load()
  }
  const openOrders = async (u: api.UserView) => {
    setOrdersOf(u)
    const r = await api.adminUserOrders(u.id, { page: 1, size: 10 })
    setOrders(r)
  }

  if (loading && !data) return <Spinner label="加载用户…" />
  const total = data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / 10))
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Input value={kw} onChange={(e) => { setKw(e.target.value); setPage(1) }} placeholder="搜索手机号 / 昵称" />
        <Button onClick={() => setPage(1)}>搜索</Button>
      </div>
      <Card className="p-0 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-bg text-muted">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">手机号</th>
              <th className="px-4 py-2 text-left">昵称</th>
              <th className="px-4 py-2 text-left">状态</th>
              <th className="px-4 py-2 text-left">注册时间</th>
              <th className="px-4 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {(data?.records ?? []).map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-2">{u.id}</td>
                <td className="px-4 py-2">{u.phone}</td>
                <td className="px-4 py-2">{u.nickname}</td>
                <td className="px-4 py-2">
                  <span className={u.status === 1 ? 'text-accent' : 'text-bad'}>{u.status === 1 ? '正常' : '已封禁'}</span>
                </td>
                <td className="px-4 py-2 text-faint">{u.createdAt ? fmtDateTime(u.createdAt) : '-'}</td>
                <td className="px-4 py-2 space-x-2">
                  <Button size="sm" variant="outline" onClick={() => openOrders(u)}>查看订单</Button>
                  {u.status === 1
                    ? <Button size="sm" variant="danger" onClick={() => toggleBan(u, true)}>封禁</Button>
                    : <Button size="sm" onClick={() => toggleBan(u, false)}>解封</Button>}
                </td>
              </tr>
            ))}
            {(data?.records ?? []).length === 0 && (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-muted">暂无用户</td></tr>
            )}
          </tbody>
        </table>
      </Card>
      <div className="flex items-center justify-between text-sm text-muted">
        <span>共 {total} 条</span>
        <div className="space-x-2">
          <Button size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</Button>
          <span>第 {page} / {pages} 页</span>
          <Button size="sm" disabled={page >= pages} onClick={() => setPage((p) => p + 1)}>下一页</Button>
        </div>
      </div>

      {ordersOf && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setOrdersOf(null)}>
          <div className="max-h-[80vh] w-full max-w-2xl overflow-auto rounded-xl bg-bg p-5" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h3 className="font-medium">用户 {ordersOf.phone} 的订单</h3>
              <Button size="sm" onClick={() => setOrdersOf(null)}>关闭</Button>
            </div>
            <div className="mt-3 space-y-2">
              {(orders?.records ?? []).map((o) => (
                <div key={o.id} className="flex items-center justify-between rounded-lg border border-line p-3 text-sm">
                  <div>
                    <div>#{o.id} · {o.status}</div>
                    <div className="text-faint">{o.serviceTitle || '上门服务'} · 商家 #{o.merchantId ?? '-'}</div>
                  </div>
                  <Money value={o.amount} className="font-semibold" />
                </div>
              ))}
              {(orders?.records ?? []).length === 0 && <div className="py-6 text-center text-muted">暂无订单</div>}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Techs() {
  const [data, setData] = useState<api.PageResult<api.TechnicianView> | null>(null)
  const [loading, setLoading] = useState(true)
  const [kw, setKw] = useState('')
  const [status, setStatus] = useState<string | undefined>()
  const [page, setPage] = useState(1)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api.adminTechnicians({ page, size: 10, keyword: kw || undefined, status })
      .then(setData).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [page, kw, status])

  const toggle = async (t: api.TechnicianView, enable: boolean) => {
    setBusy(true)
    try {
      await (enable ? api.enableTechnician(t.id) : api.disableTechnician(t.id))
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading && !data) return <Spinner label="加载技师…" />
  const total = data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / 10))
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Input value={kw} onChange={(e) => { setKw(e.target.value); setPage(1) }} placeholder="搜索姓名 / 技能" />
        <Button onClick={() => setPage(1)}>搜索</Button>
        {[undefined, 'PENDING', 'APPROVED', 'REJECTED'].map((s) => (
          <button
            key={s ?? 'all'}
            onClick={() => { setStatus(s); setPage(1) }}
            className={`rounded-full px-3 py-1.5 text-sm transition ${
              status === s ? 'bg-ink text-white' : 'bg-line/60 text-muted hover:text-ink'
            }`}
          >
            {s === undefined ? '全部' : s === 'PENDING' ? '待审核' : s === 'APPROVED' ? '已启用' : '已停用'}
          </button>
        ))}
      </div>
      <Card className="p-0 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-bg text-muted">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">姓名</th>
              <th className="px-4 py-2 text-left">技能</th>
              <th className="px-4 py-2 text-left">评分</th>
              <th className="px-4 py-2 text-left">状态</th>
              <th className="px-4 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {(data?.records ?? []).map((t) => (
              <tr key={t.id}>
                <td className="px-4 py-2">{t.id}</td>
                <td className="px-4 py-2">{t.name}</td>
                <td className="px-4 py-2">{t.skill || '-'}</td>
                <td className="px-4 py-2">{t.rating ?? 0}</td>
                <td className="px-4 py-2">
                  <span className={
                    t.status === 'APPROVED' ? 'text-accent' : t.status === 'REJECTED' ? 'text-bad' : 'text-faint'
                  }>
                    {t.status === 'APPROVED' ? '已启用' : t.status === 'REJECTED' ? '已停用' : '待审核'}
                  </span>
                </td>
                <td className="px-4 py-2 space-x-2">
                  {t.status === 'APPROVED'
                    ? <Button size="sm" variant="danger" disabled={busy} onClick={() => toggle(t, false)}>
                        <Ban size={15} /> 停用
                      </Button>
                    : <Button size="sm" disabled={busy} onClick={() => toggle(t, true)}>
                        <RotateCcw size={15} /> 启用
                      </Button>}
                </td>
              </tr>
            ))}
            {(data?.records ?? []).length === 0 && (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-muted">暂无技师</td></tr>
            )}
          </tbody>
        </table>
      </Card>
      <div className="flex items-center justify-between text-sm text-muted">
        <span>共 {total} 条</span>
        <div className="space-x-2">
          <Button size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</Button>
          <span>第 {page} / {pages} 页</span>
          <Button size="sm" disabled={page >= pages} onClick={() => setPage((p) => p + 1)}>下一页</Button>
        </div>
      </div>
    </div>
  )
}

function Services() {
  const [data, setData] = useState<api.PageResult<api.ServiceView> | null>(null)
  const [loading, setLoading] = useState(true)
  const [kw, setKw] = useState('')
  const [status, setStatus] = useState<string | undefined>()
  const [page, setPage] = useState(1)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api.adminServices({ page, size: 10, keyword: kw || undefined, status })
      .then(setData).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [page, kw, status])

  const toggle = async (s: api.ServiceView, offline: boolean) => {
    setBusy(true)
    try {
      await (offline ? api.offlineService(s.id) : api.restoreService(s.id))
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading && !data) return <Spinner label="加载服务…" />
  const total = data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / 10))
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Input value={kw} onChange={(e) => { setKw(e.target.value); setPage(1) }} placeholder="搜索服务标题" />
        <Button onClick={() => setPage(1)}>搜索</Button>
        {[undefined, 'ON', 'OFF'].map((s) => (
          <button
            key={s ?? 'all'}
            onClick={() => { setStatus(s); setPage(1) }}
            className={`rounded-full px-3 py-1.5 text-sm transition ${
              status === s ? 'bg-ink text-white' : 'bg-line/60 text-muted hover:text-ink'
            }`}
          >
            {s === undefined ? '全部' : s === 'ON' ? '已上架' : '已下架'}
          </button>
        ))}
      </div>
      <Card className="p-0 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-bg text-muted">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">标题</th>
              <th className="px-4 py-2 text-left">价格</th>
              <th className="px-4 py-2 text-left">状态</th>
              <th className="px-4 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {(data?.records ?? []).map((s) => (
              <tr key={s.id}>
                <td className="px-4 py-2">{s.id}</td>
                <td className="px-4 py-2">{s.title}</td>
                <td className="px-4 py-2"><Money value={s.price} /></td>
                <td className="px-4 py-2">
                  <span className={s.status === 'ON' ? 'text-accent' : 'text-bad'}>
                    {s.status === 'ON' ? '已上架' : '已下架'}
                  </span>
                </td>
                <td className="px-4 py-2 space-x-2">
                  {s.status === 'ON'
                    ? <Button size="sm" variant="danger" disabled={busy} onClick={() => toggle(s, true)}>
                        <Ban size={15} /> 下架
                      </Button>
                    : <Button size="sm" disabled={busy} onClick={() => toggle(s, false)}>
                        <RotateCcw size={15} /> 上架
                      </Button>}
                </td>
              </tr>
            ))}
            {(data?.records ?? []).length === 0 && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-muted">暂无服务</td></tr>
            )}
          </tbody>
        </table>
      </Card>
      <div className="flex items-center justify-between text-sm text-muted">
        <span>共 {total} 条</span>
        <div className="space-x-2">
          <Button size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</Button>
          <span>第 {page} / {pages} 页</span>
          <Button size="sm" disabled={page >= pages} onClick={() => setPage((p) => p + 1)}>下一页</Button>
        </div>
      </div>
    </div>
  )
}

function buildTabs(role?: string): [Tab, string, typeof Store][] {
  const tabs: [Tab, string, typeof Store][] = [
    ['dashboard', '数据看板', LayoutDashboard],
    ['merchants', '商家审核', Store],
    ['categories', '服务分类', Tags],
    ['orders', '订单总览', Receipt],
    ['refunds', '退款仲裁', HandCoins],
    ['users', '用户管理', UsersIcon],
    ['technicians', '技师管理', Wrench],
    ['services', '服务治理', Package],
  ]
  // 管理员账号管理为超管专属
  if (role === 'SUPER_ADMIN') tabs.push(['admins', '管理员', UserCog])
  return tabs
}

function Admins() {
  const [list, setList] = useState<api.AdminView[]>([])
  const [loading, setLoading] = useState(true)
  const [phone, setPhone] = useState('')
  const [pwd, setPwd] = useState('')
  const [nick, setNick] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api.adminAdmins().then(setList).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const add = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!phone.trim() || pwd.length < 6) {
      alert('请填写合法手机号（11 位）与密码（至少 6 位）')
      return
    }
    setBusy(true)
    try {
      await api.createAdmin({ phone: phone.trim(), password: pwd, nickname: nick.trim() || undefined })
      setPhone('')
      setPwd('')
      setNick('')
      load()
    } catch (e2) {
      alert((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载管理员…" />
  return (
    <div className="grid gap-6 md:grid-cols-2">
      <Card className="p-5">
        <h3 className="font-medium">管理员列表</h3>
        <ul className="mt-3 divide-y divide-line">
          {list.map((a) => (
            <li key={a.id} className="flex items-center justify-between py-2.5 text-sm">
              <span>
                {a.nickname}{' '}
                <span className="text-faint">（{a.role === 'SUPER_ADMIN' ? '超级管理员' : '运营'}）</span>
              </span>
              <span className="text-faint">{a.phone}</span>
            </li>
          ))}
          {list.length === 0 && <li className="py-3 text-sm text-muted">暂无管理员</li>}
        </ul>
      </Card>
      <Card className="p-5">
        <h3 className="font-medium">新增运营管理员</h3>
        <form className="mt-3 space-y-3" onSubmit={add}>
          <Field label="手机号">
            <Input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="11 位手机号" inputMode="tel" />
          </Field>
          <Field label="密码">
            <Input type="password" value={pwd} onChange={(e) => setPwd(e.target.value)} placeholder="至少 6 位" />
          </Field>
          <Field label="昵称（可选）">
            <Input value={nick} onChange={(e) => setNick(e.target.value)} placeholder="展示名称" />
          </Field>
          <Button type="submit" disabled={busy}>
            {busy ? '创建中…' : '创建'}
          </Button>
        </form>
      </Card>
    </div>
  )
}
