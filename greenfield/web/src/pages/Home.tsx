import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, MapPin, Star, SlidersHorizontal, Loader2 } from 'lucide-react'
import * as api from '../lib/api'
import { Button, Card, EmptyState, Input, Spinner } from '../lib/ui'
import { DEFAULT_CITY, fmtDistance, getLocation } from '../lib/format'

const CATEGORY_IMG: Record<string, string> = {
  家政保洁: 'https://images.unsplash.com/photo-1527515637459-a575d2b5afdd?auto=format&fit=crop&w=800&q=70',
  维修安装: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?auto=format&fit=crop&w=800&q=70',
  美容美甲: 'https://images.unsplash.com/photo-1552336679-cc2d3fb505d4?auto=format&fit=crop&w=800&q=70',
  陪诊陪护: 'https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=800&q=70',
  搬家货运: 'https://images.unsplash.com/photo-1600513492483-ab5e7d3e0b5f?auto=format&fit=crop&w=800&q=70',
  家教陪练: 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=800&q=70',
}

export default function Home() {
  const nav = useNavigate()
  const [cats, setCats] = useState<api.Category[]>([])
  const [services, setServices] = useState<api.ServiceView[]>([])
  const [keyword, setKeyword] = useState('')
  const [catId, setCatId] = useState<number | undefined>()
  const [loc, setLoc] = useState<{ lng: number; lat: number; name: string }>({ ...DEFAULT_CITY, name: DEFAULT_CITY.name })
  const [locating, setLocating] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.publicCategories().then(setCats).catch(() => {})
  }, [])

  const locate = async () => {
    setLocating(true)
    const p = await getLocation()
    setLocating(false)
    if (p) setLoc({ ...p, name: '我的位置' })
    else setLoc({ ...DEFAULT_CITY, name: DEFAULT_CITY.name })
  }

  useEffect(() => {
    let alive = true
    setLoading(true)
    api
      .searchServices({
        lng: loc.lng,
        lat: loc.lat,
        categoryId: catId,
        keyword: keyword || undefined,
        page: 1,
        size: 24,
      })
      .then((r) => {
        if (alive) setServices(r.records)
      })
      .catch(() => {})
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [loc, catId, keyword])

  const cover = (name: string) =>
    CATEGORY_IMG[name] || 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=800&q=70'

  const activeCatName = useMemo(
    () => cats.find((c) => c.id === catId)?.name,
    [cats, catId],
  )

  return (
    <div className="animate-fade-in">
      {/* Hero */}
      <section className="rounded-3xl bg-ink px-6 py-10 text-white sm:px-10 sm:py-14">
        <p className="text-sm font-medium text-white/60">同城上门服务 · 附近认证师傅</p>
        <h1 className="mt-2 max-w-2xl text-3xl font-semibold leading-tight tracking-tight sm:text-4xl">
          想要点什么服务？<br className="hidden sm:block" />附近就能安排上门。
        </h1>
        <div className="mt-6 flex max-w-2xl flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-white/50" />
            <Input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="搜索服务，如：深度保洁、空调维修…"
              className="h-12 bg-white/95 pl-11 text-ink"
            />
          </div>
          <Button size="lg" onClick={() => locate()} variant="outline" className="border-white/30 bg-transparent text-white hover:bg-white/10">
            {locating ? <Loader2 size={18} className="animate-spin" /> : <MapPin size={18} />}
            {loc.name}
          </Button>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            onClick={() => setCatId(undefined)}
            className={`rounded-full px-4 py-1.5 text-sm transition ${
              !catId ? 'bg-accent text-white' : 'bg-white/10 text-white/80 hover:bg-white/20'
            }`}
          >
            全部
          </button>
          {cats.map((c) => (
            <button
              key={c.id}
              onClick={() => setCatId(c.id === catId ? undefined : c.id)}
              className={`rounded-full px-4 py-1.5 text-sm transition ${
                c.id === catId ? 'bg-accent text-white' : 'bg-white/10 text-white/80 hover:bg-white/20'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>
      </section>

      {/* 结果 */}
      <div className="mt-8 mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold">
          {activeCatName ? `${activeCatName} · ` : ''}附近可约服务
          <span className="ml-2 text-sm font-normal text-faint">{services.length} 项</span>
        </h2>
        <span className="hidden items-center gap-1.5 text-sm text-muted sm:flex">
          <SlidersHorizontal size={16} /> 按距离优先
        </span>
      </div>

      {loading ? (
        <Spinner label="正在寻找附近的师傅…" />
      ) : services.length === 0 ? (
        <EmptyState
          icon={<Search size={28} />}
          title="附近暂时没有匹配的服务"
          desc="换个分类或关键词试试，或稍后再来看新入驻的师傅。"
        />
      ) : (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((s) => (
            <Card
              key={s.id}
              className="group cursor-pointer overflow-hidden transition hover:-translate-y-0.5 hover:shadow-lg"
              onClick={() => nav(`/services/${s.id}`)}
            >
              <div className="relative h-40 overflow-hidden bg-line">
                <img
                  src={cover(activeCatName || '家政保洁')}
                  alt={s.title}
                  className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                />
                {s.distanceM != null && (
                  <span className="absolute left-3 top-3 inline-flex items-center gap-1 rounded-full bg-ink/70 px-2.5 py-1 text-xs font-medium text-white backdrop-blur">
                    <MapPin size={12} /> {fmtDistance(s.distanceM)}
                  </span>
                )}
              </div>
              <div className="p-4">
                <h3 className="font-medium leading-snug text-ink">{s.title}</h3>
                <p className="mt-1 truncate text-sm text-muted">{s.merchantName || '平台认证商家'}</p>
                <div className="mt-3 flex items-center justify-between">
                  <span className="text-lg font-semibold text-accent">
                    <span className="text-xs">¥</span>
                    {s.price.toFixed(2)}
                    <span className="ml-1 text-xs font-normal text-faint">/次</span>
                  </span>
                  <span className="inline-flex items-center gap-1 text-sm text-muted">
                    <Star size={14} className="fill-pending text-pending" />
                    {s.merchantRating ? s.merchantRating.toFixed(1) : '新'}
                  </span>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
