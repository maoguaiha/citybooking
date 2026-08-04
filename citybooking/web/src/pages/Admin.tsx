import { useEffect, useState } from 'react'
import { ShieldCheck, Store, Tags, Receipt, HandCoins, CheckCircle2, XCircle } from 'lucide-react'
import * as api from '../lib/api'
import { Button, Card, EmptyState, Field, Input, Money, Spinner, StatusBadge } from '../lib/ui'
import { fmtDateTime } from '../lib/format'

type Tab = 'merchants' | 'categories' | 'orders' | 'refunds'

const MERCHANT_STATUS: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

export default function Admin() {
  const [tab, setTab] = useState<Tab>('merchants')

  return (
    <div className="animate-fade-in">
      <h1 className="text-xl font-semibold">平台管理</h1>
      <p className="mt-1 text-sm text-muted">商家审核、分类与订单运营、退款仲裁。</p>

      <div className="mt-5 flex flex-wrap gap-1 border-b border-line">
        {([
          ['merchants', '商家审核', Store],
          ['categories', '服务分类', Tags],
          ['orders', '订单总览', Receipt],
          ['refunds', '退款仲裁', HandCoins],
        ] as [Tab, string, typeof Store][]).map(([k, label, Icon]) => (
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
        {tab === 'merchants' && <Merchants />}
        {tab === 'categories' && <Categories />}
        {tab === 'orders' && <Orders />}
        {tab === 'refunds' && <Refunds />}
      </div>
    </div>
  )
}

function Merchants() {
  const [list, setList] = useState<api.MerchantView[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const load = () => {
    setLoading(true)
    api.adminMerchants('PENDING').then(setList).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const act = async (id: number, approve: boolean) => {
    setBusy(true)
    try {
      await api.adminAudit(id, approve)
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
            </div>
            <div className="flex gap-2">
              <Button size="sm" disabled={busy} onClick={() => act(m.id, true)}>
                <CheckCircle2 size={15} /> 通过
              </Button>
              <Button size="sm" variant="danger" disabled={busy} onClick={() => act(m.id, false)}>
                <XCircle size={15} /> 拒绝
              </Button>
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

function Orders() {
  const [list, setList] = useState<api.OrderView[]>([])
  const [status, setStatus] = useState<string | undefined>()
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    setLoading(true)
    api.adminOrders(status).then(setList).catch(() => {}).finally(() => setLoading(false))
  }, [status])

  if (loading) return <Spinner label="加载订单…" />
  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-2">
        {[undefined, 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED', 'SERVICING', 'COMPLETED', 'CANCELLED', 'REFUNDED'].map((s) => (
          <button
            key={s ?? 'all'}
            onClick={() => setStatus(s)}
            className={`rounded-full px-3 py-1.5 text-sm transition ${
              status === s ? 'bg-ink text-white' : 'bg-line/60 text-muted hover:text-ink'
            }`}
          >
            {s ?? '全部'}
          </button>
        ))}
      </div>
      {list.length === 0 ? (
        <EmptyState icon={<Receipt size={28} />} title="没有符合条件的订单" />
      ) : (
        <div className="space-y-3">
          {list.map((o) => (
            <Card key={o.id} className="p-4">
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
                </div>
              </div>
            </Card>
          ))}
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
    api.adminOrders('REFUNDED').then(setList).catch(() => {}).finally(() => setLoading(false))
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
            </div>
          </div>
        </Card>
      ))}
    </div>
  )
}
