import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import * as api from '../lib/api'
import { useAuth } from '../lib/auth'
import { Button, Card, EmptyState, Field, IconButton, Money, Spinner, StatusBadge, Textarea } from '../lib/ui'
import { fmtTime } from '../lib/format'

const STEPS = ['UNPAID', 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED', 'SERVICING', 'COMPLETED']

export default function OrderDetail() {
  const { id } = useParams()
  const oid = Number(id)
  const nav = useNavigate()
  const { user } = useAuth()
  const role = user?.role

  const [order, setOrder] = useState<api.OrderView | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [reviewOpen, setReviewOpen] = useState(false)
  const [score, setScore] = useState(5)
  const [comment, setComment] = useState('')

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setOrder(await api.orderDetail(oid))
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setLoading(false)
    }
  }, [oid])

  useEffect(() => {
    load()
  }, [load])

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      setBusy(true)
      await fn()
      setErr(null)
      await load()
    } catch (e) {
      setErr((e as Error).message || ok)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="加载订单中…" />
  if (err && !order) return <EmptyState title="加载失败" desc={err} action={<Button onClick={load}>重试</Button>} />
  if (!order) return <EmptyState title="订单不存在" />

  const isConsumer = role === 'CONSUMER'
  const isMerchant = role === 'MERCHANT' || role === 'TECHNICIAN'

  const submitReview = () => run(() => api.reviewOrder(oid, score, comment), '评价失败').then(() => setReviewOpen(false))

  return (
    <div className="mx-auto max-w-2xl px-4 py-5">
      <div className="mb-4 flex items-center gap-2">
        <IconButton onClick={() => nav(-1)} aria-label="返回">
          <ArrowLeft size={20} />
        </IconButton>
        <h1 className="text-lg font-semibold text-ink">订单详情</h1>
        <div className="ml-auto">
          <StatusBadge status={order.status} />
        </div>
      </div>

      {err && (
        <div className="mb-3 rounded-xl border border-bad/40 bg-bad-soft px-3 py-2 text-sm text-bad">{err}</div>
      )}

      <Card className="space-y-3 p-4">
        <div>
          <p className="text-base font-medium text-ink">{order.serviceTitle}</p>
          <p className="mt-0.5 text-sm text-muted">订单号 {order.orderNo}</p>
        </div>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-faint">金额</p>
            <Money value={order.amount} className="font-semibold text-ink" />
          </div>
          <div>
            <p className="text-faint">服务模式</p>
            <p className="text-ink">{order.mode === 'GRAB' ? '平台抢单' : '指定服务'}</p>
          </div>
          <div className="col-span-2">
            <p className="text-faint">上门地址</p>
            <p className="text-ink">{order.address || '尽快上门'}</p>
          </div>
          <div className="col-span-2">
            <p className="text-faint">预约时间</p>
            <p className="text-ink">{fmtTime(order.appointmentTime)}</p>
          </div>
          <div className="col-span-2 grid grid-cols-3 gap-2 text-xs text-muted">
            <div>支付：{order.payStatus}</div>
            <div>退款：{order.refundStatus}</div>
            <div>下单：{fmtTime(order.createdAt)}</div>
          </div>
        </div>
      </Card>

      <Card className="mt-3 p-4">
        <p className="mb-3 text-sm font-medium text-ink">服务进度</p>
        <ol className="space-y-2">
          {STEPS.map((s) => {
            const reached = STEPS.indexOf(order.status) >= STEPS.indexOf(s) || order.status === 'CANCELLED' && s === 'UNPAID'
            const active = order.status === s
            return (
              <li key={s} className="flex items-center gap-3">
                <span
                  className={`h-2.5 w-2.5 rounded-full ${active ? 'bg-accent' : reached ? 'bg-ok' : 'bg-line'}`}
                />
                <span className={active ? 'text-sm font-medium text-ink' : 'text-sm text-muted'}>
                  <StatusBadge status={s} />
                </span>
              </li>
            )
          })}
        </ol>
      </Card>

      <div className="mt-4 flex flex-wrap gap-2">
        {isConsumer && (order.status === 'UNPAID') && (
          <Button onClick={() => run(() => api.payOrder(oid), '支付失败')} disabled={busy}>
            去支付
          </Button>
        )}
        {isConsumer && ['UNPAID', 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED', 'SERVICING'].includes(order.status) && (
          <Button variant="outline" onClick={() => run(() => api.cancelOrder(oid), '取消失败')} disabled={busy}>
            取消订单
          </Button>
        )}
        {isConsumer && order.status === 'COMPLETED' && (
          <Button variant="outline" onClick={() => setReviewOpen((v) => !v)} disabled={busy}>
            评价
          </Button>
        )}

        {isMerchant && order.status === 'WAIT_ACCEPT' && (
          <Button onClick={() => run(() => api.acceptOrder(oid), '接单失败')} disabled={busy}>
            接单
          </Button>
        )}
        {isMerchant && order.status === 'PENDING_GRAB' && (
          <Button onClick={() => run(() => api.grabOrder(oid), '抢单失败')} disabled={busy}>
            抢单
          </Button>
        )}
        {isMerchant && order.status === 'ACCEPTED' && (
          <Button onClick={() => run(() => api.startOrder(oid), '开始失败')} disabled={busy}>
            开始服务
          </Button>
        )}
        {isMerchant && order.status === 'SERVICING' && (
          <Button onClick={() => run(() => api.completeOrder(oid), '完成失败')} disabled={busy}>
            完成服务
          </Button>
        )}
      </div>

      {reviewOpen && (
        <Card className="mt-3 space-y-3 p-4">
          <Field label="评分">
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  onClick={() => setScore(n)}
                  className={`h-9 w-9 rounded-full text-lg ${n <= score ? 'bg-accent text-white' : 'bg-line text-muted'}`}
                >
                  {n}
                </button>
              ))}
            </div>
          </Field>
          <Field label="评价">
            <Textarea value={comment} onChange={(e) => setComment(e.target.value)} placeholder="说点什么…" />
          </Field>
          <div className="flex gap-2">
            <Button onClick={submitReview} disabled={busy}>
              提交评价
            </Button>
            <Button variant="ghost" onClick={() => setReviewOpen(false)}>
              取消
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}
