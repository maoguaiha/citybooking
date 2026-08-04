import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, MapPin, Star, Clock, CheckCircle2, Wallet } from 'lucide-react'
import * as api from '../lib/api'
import { Button, Card, EmptyState, Field, Input, Money, Spinner, StatusBadge } from '../lib/ui'
import { DEFAULT_CITY, fmtTime, getLocation } from '../lib/format'

export default function ServiceDetail() {
  const { id } = useParams()
  const nav = useNavigate()
  const [svc, setSvc] = useState<api.ServiceView | null>(null)
  const [loading, setLoading] = useState(true)
  const [mode, setMode] = useState<'APPOINT' | 'GRAB'>('APPOINT')
  const [address, setAddress] = useState('')
  const [when, setWhen] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [paid, setPaid] = useState<api.PayResp | null>(null)

  useEffect(() => {
    if (!id) return
    api
      .serviceDetail(Number(id))
      .then(setSvc)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Spinner label="加载服务详情…" />
  if (!svc) return <EmptyState title="服务不存在或已下架" action={<Button onClick={() => nav('/')}>返回首页</Button>} />

  const book = async () => {
    setErr('')
    if (!address.trim()) return setErr('请填写上门地址')
    setBusy(true)
    try {
      const loc = await getLocation()
      const orderId = await api.createOrder({
        serviceId: svc.id,
        mode,
        merchantId: mode === 'APPOINT' ? svc.merchantId : undefined,
        address: address.trim(),
        lng: loc?.lng ?? DEFAULT_CITY.lng,
        lat: loc?.lat ?? DEFAULT_CITY.lat,
        appointmentTime: when ? new Date(when).toISOString().slice(0, 19) : undefined,
      })
      const pay = await api.payOrder(orderId)
      setPaid(pay)
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (paid) {
    return (
      <Card className="mx-auto max-w-md p-8 text-center animate-fade-in">
        <CheckCircle2 size={48} className="mx-auto text-ok" />
        <h2 className="mt-4 text-xl font-semibold">支付成功</h2>
        <p className="mt-1 text-sm text-muted">
          已通过模拟支付完成，金额 <Money value={paid.amount} className="font-medium text-ink" />，
          渠道 {paid.channel}。
        </p>
        <p className="mt-3 text-sm text-muted">
          订单状态：
          <span className="ml-1 font-medium text-ink">
            {mode === 'APPOINT' ? '待商家接单' : '抢单中，附近师傅可抢单'}
          </span>
        </p>
        <div className="mt-6 flex gap-3">
          <Button className="flex-1" onClick={() => nav('/orders')}>
            查看我的订单
          </Button>
          <Button variant="outline" className="flex-1" onClick={() => nav('/')}>
            继续逛逛
          </Button>
        </div>
      </Card>
    )
  }

  return (
    <div className="animate-fade-in">
      <button onClick={() => nav(-1)} className="mb-4 inline-flex items-center gap-1 text-sm text-muted hover:text-ink">
        <ArrowLeft size={16} /> 返回
      </button>

      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <div>
          <div className="relative h-56 overflow-hidden rounded-2xl bg-line">
            <img
              src="https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=1200&q=70"
              alt={svc.title}
              className="h-full w-full object-cover"
            />
          </div>
          <h1 className="mt-5 text-2xl font-semibold tracking-tight">{svc.title}</h1>
          <div className="mt-2 flex items-center gap-4 text-sm text-muted">
            <span className="inline-flex items-center gap-1.5">
              <MapPin size={15} /> {svc.merchantName || '平台认证商家'}
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Star size={15} className="fill-pending text-pending" />
              {svc.merchantRating ? svc.merchantRating.toFixed(1) : '新店'}
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Clock size={15} /> 约 {svc.durationMin} 分钟
            </span>
          </div>
          <p className="mt-4 leading-relaxed text-ink/80">{svc.description || '专业师傅上门服务，明码标价，服务过程全程可追溯。'}</p>
        </div>

        <div>
          <Card className="sticky top-20 p-5">
            <div className="flex items-baseline justify-between">
              <span className="text-sm text-muted">服务价格</span>
              <Money value={svc.price} className="text-2xl font-semibold text-accent" />
            </div>

            <div className="mt-4">
              <span className="mb-2 block text-sm font-medium">预约方式</span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setMode('APPOINT')}
                  className={`rounded-xl border p-3 text-left text-sm transition ${
                    mode === 'APPOINT' ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'
                  }`}
                >
                  <span className="block font-medium">指定商家</span>
                  <span className="block text-xs text-faint">由该商家接单</span>
                </button>
                <button
                  onClick={() => setMode('GRAB')}
                  className={`rounded-xl border p-3 text-left text-sm transition ${
                    mode === 'GRAB' ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'
                  }`}
                >
                  <span className="block font-medium">智能抢单</span>
                  <span className="block text-xs text-faint">附近师傅抢单</span>
                </button>
              </div>
            </div>

            <div className="mt-4 space-y-3">
              <Field label="上门地址">
                <Input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="如：朝阳区 xx 小区 3 栋" />
              </Field>
              <Field label="期望上门时间" hint="留空表示尽快">
                <Input type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} />
              </Field>
            </div>

            {err && <p className="mt-3 text-sm text-bad">{err}</p>}

            <Button size="lg" className="mt-5 w-full" onClick={book} disabled={busy}>
              {busy ? '提交中…' : <><Wallet size={18} /> 立即预约并支付</>}
            </Button>
            <p className="mt-3 text-center text-xs text-faint">当前为模拟支付环境，不会产生真实扣款</p>
          </Card>
        </div>
      </div>
    </div>
  )
}
