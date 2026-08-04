import { useEffect, useState } from 'react'
import { Store, Zap, ListChecks, Plus, Tag, MapPin, Clock, CheckCircle2, PlayCircle, Hand } from 'lucide-react'
import * as api from '../lib/api'
import { Button, Card, EmptyState, Field, Input, Money, Spinner, StatusBadge, Textarea } from '../lib/ui'
import { DEFAULT_CITY, fmtTime, getLocation } from '../lib/format'

type Tab = 'grab' | 'orders' | 'services' | 'team'

export default function Merchant() {
  const [tab, setTab] = useState<Tab>('grab')
  const [onboarded, setOnboarded] = useState<boolean | null>(null)
  const [profile, setProfile] = useState<api.MerchantView | null>(null)

  useEffect(() => {
    api
      .merchantProfile()
      .then((p) => {
        setProfile(p)
        setOnboarded(true)
      })
      .catch(() => setOnboarded(false))
  }, [])

  if (onboarded === null) return <Spinner label="加载工作台…" />
  if (!onboarded) return <Onboard onDone={(p) => { setProfile(p); setOnboarded(true) }} />

  return (
    <div className="animate-fade-in">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">{profile?.name || '商家工作台'}</h1>
          <p className="mt-1 text-sm text-muted">
            {profile ? `状态 ${profile.status === 'APPROVED' ? '已认证' : profile.status} · 评分 ${profile.rating || '—'}` : ''}
          </p>
        </div>
      </div>

      <div className="mt-5 flex gap-1 border-b border-line">
        {([
          ['grab', '抢单看板', Zap],
          ['orders', '我的订单', ListChecks],
          ['services', '服务项目', Tag],
          ['team', '团队/入驻', Store],
        ] as [Tab, string, typeof Zap][]).map(([k, label, Icon]) => (
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
        {tab === 'grab' && <GrabBoard />}
        {tab === 'orders' && <MerchantOrders />}
        {tab === 'services' && <MerchantServices />}
        {tab === 'team' && profile && <Team profile={profile} />}
      </div>
    </div>
  )
}

function Onboard({ onDone }: { onDone: (p: api.MerchantView) => void }) {
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [loc, setLoc] = useState(DEFAULT_CITY)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    if (!name.trim()) return setErr('请填写店铺名称')
    setBusy(true)
    try {
      const p = await getLocation()
      const lng = p?.lng ?? DEFAULT_CITY.lng
      const lat = p?.lat ?? DEFAULT_CITY.lat
      await api.onboard({ name: name.trim(), address: address.trim() || undefined, lng, lat, radius: 5000 })
      setLoc({ ...loc, lng, lat })
      onDone({ id: 0, name: name.trim(), address: address || null, lng, lat, radius: 5000, status: 'PENDING', rating: 0 })
    } catch (e2) {
      setErr((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card className="mx-auto max-w-md p-6 animate-fade-in">
      <h2 className="text-lg font-semibold">入驻邻约</h2>
      <p className="mt-1 text-sm text-muted">填写店铺信息，提交后等待平台审核即可开始接单。</p>
      <form className="mt-5 space-y-4" onSubmit={submit}>
        <Field label="店铺名称">
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="如：好邻家政（望京店）" />
        </Field>
        <Field label="经营地址">
          <Input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="如：朝阳区望京 SOHO" />
        </Field>
        <p className="flex items-center gap-1.5 text-xs text-faint">
          <MapPin size={13} /> 将使用当前定位作为服务圆心（半径 5km）
        </p>
        {err && <p className="text-sm text-bad">{err}</p>}
        <Button type="submit" size="lg" className="w-full" disabled={busy}>
          {busy ? '提交中…' : '提交入驻'}
        </Button>
      </form>
    </Card>
  )
}

function GrabBoard() {
  const [items, setItems] = useState<api.OrderView[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const load = () => {
    setLoading(true)
    api.grabBoard().then(setItems).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  if (loading) return <Spinner label="加载可抢订单…" />
  if (items.length === 0)
    return <EmptyState icon={<Zap size={28} />} title="暂无可抢订单" desc="附近暂无智能抢单模式的订单，有新单会实时推送。" />

  return (
    <div className="space-y-4">
      {items.map((o) => (
        <Card key={o.id} className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="font-medium text-ink">{o.serviceTitle || '上门服务'}</h3>
              <p className="mt-1 text-sm text-muted inline-flex items-center gap-1.5">
                <MapPin size={14} /> {o.address || '未填写地址'}
              </p>
              <p className="mt-1 text-sm text-muted inline-flex items-center gap-1.5">
                <Clock size={14} /> 期望 {fmtTime(o.appointmentTime)}
              </p>
            </div>
            <Money value={o.amount} className="text-lg font-semibold text-accent" />
          </div>
          <div className="mt-4 flex justify-end">
            <Button
              size="sm"
              disabled={busy}
              onClick={async () => {
                setBusy(true)
                try {
                  await api.grabOrder(o.id)
                  load()
                } catch (e) {
                  alert((e as Error).message)
                } finally {
                  setBusy(false)
                }
              }}
            >
              <Hand size={15} /> 抢下此单
            </Button>
          </div>
        </Card>
      ))}
    </div>
  )
}

function MerchantOrders() {
  const [items, setItems] = useState<api.OrderView[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const load = () => {
    setLoading(true)
    api.myOrders().then((r) => setItems(r.records)).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const act = async (fn: () => Promise<unknown>) => {
    setBusy(true)
    try {
      await fn()
      load()
    } catch (e) {
      alert((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载订单…" />
  if (items.length === 0) return <EmptyState icon={<ListChecks size={28} />} title="还没有订单" desc="接单或抢单后，订单会出现在这里。" />

  return (
    <div className="space-y-4">
      {items.map((o) => (
        <Card key={o.id} className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-medium text-ink">{o.serviceTitle || '上门服务'}</h3>
                <StatusBadge status={o.status} />
              </div>
              <p className="mt-1 text-sm text-muted inline-flex items-center gap-1.5">
                <MapPin size={14} /> {o.address || '未填写地址'}
              </p>
              <p className="mt-1 text-sm text-muted">下单 {fmtTime(o.createdAt)} · 方式 {o.mode === 'APPOINT' ? '指定' : '抢单'}</p>
            </div>
            <Money value={o.amount} className="text-lg font-semibold text-ink" />
          </div>
          <div className="mt-4 flex flex-wrap justify-end gap-2">
            {o.status === 'WAIT_ACCEPT' && (
              <Button size="sm" disabled={busy} onClick={() => act(() => api.acceptOrder(o.id))}>
                <CheckCircle2 size={15} /> 接单
              </Button>
            )}
            {o.status === 'ACCEPTED' && (
              <Button size="sm" disabled={busy} onClick={() => act(() => api.startOrder(o.id))}>
                <PlayCircle size={15} /> 开始服务
              </Button>
            )}
            {o.status === 'SERVICING' && (
              <Button size="sm" disabled={busy} onClick={() => act(() => api.completeOrder(o.id))}>
                <CheckCircle2 size={15} /> 完成服务
              </Button>
            )}
          </div>
        </Card>
      ))}
    </div>
  )
}

function MerchantServices() {
  const [items, setItems] = useState<api.ServiceView[]>([])
  const [cats, setCats] = useState<api.Category[]>([])
  const [showForm, setShowForm] = useState(false)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [form, setForm] = useState({ categoryId: 0, title: '', description: '', price: '', durationMin: '60' })

  const load = () => {
    setLoading(true)
    Promise.all([api.myServices(), api.publicCategories()])
      .then(([s, c]) => {
        setItems(s)
        setCats(c)
        if (c.length && form.categoryId === 0) setForm((f) => ({ ...f, categoryId: c[0].id }))
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    setBusy(true)
    try {
      await api.createService({
        categoryId: form.categoryId,
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        price: Number(form.price),
        durationMin: Number(form.durationMin),
      })
      setShowForm(false)
      setForm({ categoryId: cats[0]?.id || 0, title: '', description: '', price: '', durationMin: '60' })
      load()
    } catch (e2) {
      setErr((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载服务项目…" />

  return (
    <div>
      <div className="flex justify-end">
        <Button size="sm" onClick={() => setShowForm((v) => !v)}>
          <Plus size={15} /> 新增服务
        </Button>
      </div>

      {showForm && (
        <Card className="mt-4 p-5">
          <form className="space-y-4" onSubmit={submit}>
            <Field label="所属分类">
              <select
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}
                className="h-11 w-full rounded-xl border border-line bg-surface px-4 text-sm outline-none focus:border-accent/60"
              >
                {cats.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="服务名称">
              <Input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="如：深度保洁 3 小时" />
            </Field>
            <Field label="服务描述">
              <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} placeholder="简单介绍服务内容" />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="价格 (¥)">
                <Input type="number" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} placeholder="99" />
              </Field>
              <Field label="时长 (分钟)">
                <Input type="number" value={form.durationMin} onChange={(e) => setForm({ ...form, durationMin: e.target.value })} />
              </Field>
            </div>
            {err && <p className="text-sm text-bad">{err}</p>}
            <div className="flex gap-2">
              <Button type="submit" disabled={busy}>
                {busy ? '保存中…' : '保存'}
              </Button>
              <Button type="button" variant="ghost" onClick={() => setShowForm(false)}>
                取消
              </Button>
            </div>
          </form>
        </Card>
      )}

      {items.length === 0 ? (
        <EmptyState icon={<Tag size={28} />} title="还没有上架服务" desc="新增你的第一个服务项目，开始接单。" />
      ) : (
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {items.map((s) => (
            <Card key={s.id} className="p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-medium text-ink">{s.title}</h3>
                  <p className="mt-1 text-sm text-muted line-clamp-2">{s.description || '—'}</p>
                </div>
                <StatusBadge status={s.status} />
              </div>
              <div className="mt-3 flex items-center justify-between text-sm">
                <Money value={s.price} className="font-semibold text-accent" />
                <span className="text-faint">约 {s.durationMin} 分钟</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

function Team({ profile }: { profile: api.MerchantView }) {
  const [techs, setTechs] = useState<{ name: string; skill?: string }[]>([])
  const [name, setName] = useState('')
  const [skill, setSkill] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const add = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    if (!name.trim()) return setErr('请填写技师姓名')
    setBusy(true)
    try {
      await api.addTechnician({ name: name.trim(), skill: skill.trim() || undefined, lng: profile.lng, lat: profile.lat })
      setTechs((t) => [...t, { name: name.trim(), skill: skill.trim() || undefined }])
      setName('')
      setSkill('')
    } catch (e2) {
      setErr((e2 as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <Card className="p-5">
        <h3 className="font-medium">店铺资料</h3>
        <dl className="mt-3 space-y-2 text-sm">
          <div className="flex justify-between"><dt className="text-muted">名称</dt><dd>{profile.name}</dd></div>
          <div className="flex justify-between"><dt className="text-muted">地址</dt><dd className="max-w-[60%] text-right">{profile.address || '—'}</dd></div>
          <div className="flex justify-between"><dt className="text-muted">状态</dt><dd>{profile.status}</dd></div>
          <div className="flex justify-between"><dt className="text-muted">评分</dt><dd>{profile.rating || '—'}</dd></div>
          <div className="flex justify-between"><dt className="text-muted">服务半径</dt><dd>{profile.radius ? `${profile.radius}m` : '—'}</dd></div>
        </dl>
      </Card>

      <Card className="p-5">
        <h3 className="font-medium">添加技师</h3>
        <form className="mt-3 space-y-3" onSubmit={add}>
          <Field label="姓名">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="技师姓名" />
          </Field>
          <Field label="擅长">
            <Input value={skill} onChange={(e) => setSkill(e.target.value)} placeholder="如：空调维修" />
          </Field>
          {err && <p className="text-sm text-bad">{err}</p>}
          <Button type="submit" disabled={busy}>
            {busy ? '添加中…' : '添加技师'}
          </Button>
        </form>
        {techs.length > 0 && (
          <ul className="mt-4 space-y-2">
            {techs.map((t, i) => (
              <li key={i} className="flex items-center gap-2 text-sm">
                <span className="grid h-7 w-7 place-items-center rounded-full bg-accent-soft text-xs font-medium text-accent">
                  {t.name[0]}
                </span>
                {t.name}
                {t.skill && <span className="text-faint">· {t.skill}</span>}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
