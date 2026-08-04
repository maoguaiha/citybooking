import type { ButtonHTMLAttributes, InputHTMLAttributes, MouseEventHandler, ReactNode, TextareaHTMLAttributes } from 'react'
import { Loader2 } from 'lucide-react'

type Variant = 'primary' | 'ghost' | 'outline' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const base =
  'inline-flex items-center justify-center gap-2 rounded-full font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40'

const variants: Record<Variant, string> = {
  primary: 'bg-accent text-white hover:bg-accent-press',
  ghost: 'text-ink hover:bg-line/60',
  outline: 'border border-line text-ink hover:border-ink/40 hover:bg-line/40',
  danger: 'bg-bad text-white hover:bg-bad/90',
}

const sizes: Record<Size, string> = {
  sm: 'h-9 px-4 text-sm',
  md: 'h-11 px-6 text-sm',
  lg: 'h-12 px-7 text-base',
}

export function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: Size }) {
  return (
    <button className={`${base} ${variants[variant]} ${sizes[size]} ${className}`} {...rest}>
      {children}
    </button>
  )
}

export function IconButton({
  className = '',
  children,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={`inline-flex h-10 w-10 items-center justify-center rounded-full text-ink hover:bg-line/60 transition-colors ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
}

export function Card({
  className = '',
  children,
  onClick,
}: {
  className?: string
  children: ReactNode
  onClick?: MouseEventHandler<HTMLDivElement>
}) {
  return (
    <div onClick={onClick} className={`rounded-2xl border border-line bg-surface ${className}`}>
      {children}
    </div>
  )
}

export function Input({ className = '', ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`h-11 w-full rounded-xl border border-line bg-surface px-4 text-sm text-ink placeholder:text-faint outline-none transition focus:border-accent/60 focus:ring-2 focus:ring-accent/20 ${className}`}
      {...rest}
    />
  )
}

export function Textarea({ className = '', ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={`w-full rounded-xl border border-line bg-surface px-4 py-3 text-sm text-ink placeholder:text-faint outline-none transition focus:border-accent/60 focus:ring-2 focus:ring-accent/20 ${className}`}
      {...rest}
    />
  )
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-faint">{hint}</span>}
    </label>
  )
}

export function Badge({
  children,
  tone = 'neutral',
}: {
  children: ReactNode
  tone?: 'neutral' | 'accent' | 'pending' | 'ok' | 'bad'
}) {
  const tones: Record<string, string> = {
    neutral: 'bg-line text-muted',
    accent: 'bg-accent-soft text-accent',
    pending: 'bg-pending-soft text-pending',
    ok: 'bg-ok-soft text-ok',
    bad: 'bg-bad-soft text-bad',
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}>
      {children}
    </span>
  )
}

const STATUS_META: Record<string, { label: string; tone: 'neutral' | 'accent' | 'pending' | 'ok' | 'bad' }> = {
  UNPAID: { label: '待支付', tone: 'pending' },
  WAIT_ACCEPT: { label: '待接单', tone: 'pending' },
  PENDING_GRAB: { label: '抢单中', tone: 'accent' },
  ACCEPTED: { label: '已接单', tone: 'accent' },
  SERVICING: { label: '服务中', tone: 'accent' },
  COMPLETED: { label: '已完成', tone: 'ok' },
  CANCELLED: { label: '已取消', tone: 'bad' },
  REFUNDED: { label: '已退款', tone: 'bad' },
}

export function StatusBadge({ status }: { status: string }) {
  const m = STATUS_META[status] || { label: status, tone: 'neutral' as const }
  return <Badge tone={m.tone}>{m.label}</Badge>
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-16 text-muted">
      <Loader2 className="animate-spin" size={18} />
      {label && <span className="text-sm">{label}</span>}
    </div>
  )
}

export function EmptyState({ icon, title, desc, action }: { icon?: ReactNode; title: string; desc?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-line bg-surface/60 px-6 py-16 text-center">
      {icon && <div className="mb-3 text-faint">{icon}</div>}
      <p className="text-sm font-medium text-ink">{title}</p>
      {desc && <p className="mt-1 max-w-xs text-sm text-muted">{desc}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function Money({ value, className = '' }: { value: number; className?: string }) {
  return (
    <span className={className}>
      <span className="text-xs">¥</span>
      {value.toFixed(2)}
    </span>
  )
}
