import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Package, MapPin, Clock, Star, XCircle, Wallet, MessageSquare } from 'lucide-react'
import * as api from '../lib/api'
import { Button, Card, EmptyState, Field, Money, Spinner, StatusBadge, Textarea } from '../lib/ui'
import { fmtDateTime, fmtTime } from '../lib/format'

const canCancel = (s: string) => ['UNPAID', 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED'].includes(s)
const canPay = (s: string) => s === 'UNPAID'
const canReview = (s: string) => s === 'COMPLETED'

export default function Orders() {
  const nav = useNavigate()
  const [orders, setOrders] = useState<api.OrderView[]>([])
  const [loading, setLoading] = useState(true)
  const [reviewing, setReviewing] = useState<api.OrderView | null>(null)
  const [score, setScore] = useState(5)
  const [comment, setComment] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    api
      .myOrders()
      .then((r) => setOrders(r.records))
      .catch(() => {})
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const act = async (fn: () => Promise<unknown>, after?: () => void) => {
    setBusy(true)
    setErr('')
    try {
      await fn()
      after ? after() : load()
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const submitReview = () =>
    reviewing &&
    act(async () => {
      await api.reviewOrder(reviewing.id, score, comment)
      setReviewing(null)
      setComment('')
      setScore(5)
    })

  if (loading) return <Spinner label="加载订单…" />

  return (
    <div className="animate-fade-in">
      <h1 className="text-xl font-semibold">我的订单</h1>
      <p className="mt-1 text-sm text-muted">查看预约进度，未服务可取消（按阶段阶梯退款）。</p>

      {err && <p className="mt-3 text-sm text-bad">{err}</p>}

      {orders.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            icon={<Package size={28} />}
            title="还没有订单"
            desc="去首页看看附近能约什么服务吧。"
            action={<Button onClick={() => nav('/')}>去找服务</Button>}
          />
        </div>
      ) : (
        <div className="mt-6 space-y-4">
          {orders.map((o) => (
            <Card key={o.id} className="cursor-pointer p-5 transition hover:border-accent/40" onClick={() => nav('/orders/' + o.id)}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium text-ink">{o.serviceTitle || '上门服务'}</h3>
                    <StatusBadge status={o.status} />
                  </div>
                  <p className="mt-1 text-xs text-faint">订单号 {o.orderNo}</p>
                  <div className="mt-2 space-y-1 text-sm text-muted">
                    <p className="inline-flex items-center gap-1.5">
                      <MapPin size={14} /> {o.address || '未填写地址'}
                    </p>
                    <p className="inline-flex items-center gap-1.5">
                      <Clock size={14} /> 期望 {fmtTime(o.appointmentTime)}
                    </p>
                    <p>方式：{o.mode === 'APPOINT' ? '指定商家' : '智能抢单'}</p>
                  </div>
                </div>
                <div className="text-right">
                  <Money value={o.amount} className="text-lg font-semibold text-ink" />
                  <p className="mt-1 text-xs text-faint">下单 {fmtDateTime(o.createdAt)}</p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2" onClick={(e) => e.stopPropagation()}>
                {canPay(o.status) && (
                  <Button size="sm" onClick={() => act(() => api.payOrder(o.id))} disabled={busy}>
                    <Wallet size={15} /> 去支付
                  </Button>
                )}
                {canCancel(o.status) && (
                  <Button size="sm" variant="outline" onClick={() => act(() => api.cancelOrder(o.id))} disabled={busy}>
                    <XCircle size={15} /> 取消订单
                  </Button>
                )}
                {canReview(o.status) && (
                  <Button size="sm" onClick={() => setReviewing(o)}>
                    <MessageSquare size={15} /> 评价
                  </Button>
                )}
                <Button size="sm" variant="ghost" onClick={() => nav('/orders/' + o.id)}>
                  查看详情
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {reviewing && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-ink/40 p-4" onClick={() => setReviewing(null)}>
          <Card className="w-full max-w-sm p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold">评价服务</h3>
            <p className="mt-1 text-sm text-muted">{reviewing.serviceTitle}</p>
            <div className="mt-4 flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} onClick={() => setScore(n)}>
                  <Star
                    size={26}
                    className={n <= score ? 'fill-pending text-pending' : 'text-line'}
                  />
                </button>
              ))}
            </div>
            <div className="mt-3">
              <Field label="评语">
                <Textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} placeholder="说说这次的体验…" />
              </Field>
            </div>
            <div className="mt-4 flex gap-2">
              <Button className="flex-1" onClick={submitReview} disabled={busy}>
                提交评价
              </Button>
              <Button variant="ghost" onClick={() => setReviewing(null)}>
                取消
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
