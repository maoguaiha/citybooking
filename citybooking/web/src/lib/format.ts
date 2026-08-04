export function fmtDistance(m: number | null | undefined): string {
  if (m == null) return ''
  if (m < 1000) return `${Math.round(m)}m`
  return `${(m / 1000).toFixed(1)}km`
}

export function fmtTime(s: string | null | undefined): string {
  if (!s) return '尽快上门'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const p = (n: number) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

export function fmtDateTime(s: string | null | undefined): string {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const p = (n: number) => `${n}`.padStart(2, '0')
  return `${d.getMonth() + 1}月${d.getDate()}日 ${p(d.getHours())}:${p(d.getMinutes())}`
}

export const DEFAULT_CITY = { lng: 116.397, lat: 39.908, name: '北京' }

export async function getLocation(): Promise<{ lng: number; lat: number } | null> {
  return new Promise((resolve) => {
    if (!('geolocation' in navigator)) return resolve(null)
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lng: +pos.coords.longitude.toFixed(6), lat: +pos.coords.latitude.toFixed(6) }),
      () => resolve(null),
      { timeout: 6000 },
    )
  })
}
