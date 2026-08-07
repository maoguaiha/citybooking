import axios, { type AxiosRequestConfig } from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

const client = axios.create({ baseURL: '/api', timeout: 15000 })

client.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('cb_token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

client.interceptors.response.use(
  (r) => r,
  (err) => {
    const data = err?.response?.data
    const msg = data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const r = await client.request<ApiResponse<T>>(config)
  const body = r.data
  if (body.code !== 0) throw new Error(body.message || '请求失败')
  return body.data
}

/* ---------- 类型 ---------- */
export interface AuthResp {
  userId: number
  token: string
  role: string
}
export interface UserInfo {
  id: number
  phone: string
  nickname: string
  role: string
  status: number
}
export interface ServiceView {
  id: number
  merchantId: number
  technicianId: number | null
  categoryId: number
  title: string
  description: string | null
  price: number
  durationMin: number
  availableStart: string | null
  availableEnd: string | null
  status: string
  merchantName: string
  merchantRating: number
  distanceM: number | null
}
export interface MerchantView {
  id: number
  name: string
  address: string | null
  lng: number
  lat: number
  radius: number | null
  status: string
  rating: number
  rejectReason?: string | null
}
export interface OrderView {
  id: number
  orderNo: string
  consumerId: number
  merchantId: number | null
  technicianId: number | null
  serviceId: number
  serviceTitle: string
  mode: string
  address: string | null
  lng: number | null
  lat: number | null
  appointmentTime: string | null
  amount: number
  status: string
  payStatus: string
  refundStatus: string
  createdAt: string
}
export interface PayResp {
  orderId: number
  paid: boolean
  amount: number
  tradeNo: string
  channel: string
}
export interface Category {
  id: number
  name: string
  parentId: number
  sort: number
}
export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

/* ---------- 鉴权 ---------- */
export const register = (phone: string, password: string, nickname: string, role: string) =>
  request<AuthResp>({ method: 'POST', url: '/auth/register', data: { phone, password, nickname, role } })

export const login = (phone: string, password: string) =>
  request<AuthResp>({ method: 'POST', url: '/auth/login', data: { phone, password } })

export const me = () => request<UserInfo>({ method: 'GET', url: '/auth/me' })

/* ---------- 服务 / LBS ---------- */
export const searchServices = (params: {
  lng?: number
  lat?: number
  radius?: number
  categoryId?: number
  keyword?: string
  page?: number
  size?: number
}) => request<PageResult<ServiceView>>({ method: 'GET', url: '/services', params })

export const serviceDetail = (id: number) =>
  request<ServiceView>({ method: 'GET', url: `/services/${id}` })

export const categories = () => request<Category[]>({ method: 'GET', url: '/admin/categories' })

export const publicCategories = () => request<Category[]>({ method: 'GET', url: '/services/categories' })

/* ---------- 订单 ---------- */
export const createOrder = (data: {
  serviceId: number
  mode: 'APPOINT' | 'GRAB'
  merchantId?: number
  technicianId?: number
  address?: string
  lng?: number
  lat?: number
  appointmentTime?: string
}) => request<number>({ method: 'POST', url: '/orders', data })

export const payOrder = (id: number) => request<PayResp>({ method: 'POST', url: `/orders/${id}/pay` })

export const orderDetail = (id: number) =>
  request<OrderView>({ method: 'GET', url: `/orders/${id}` })

export const myOrders = (status?: string) =>
  request<PageResult<OrderView>>({
    method: 'GET',
    url: '/orders',
    params: { status, page: 1, size: 50 },
  })

export const cancelOrder = (id: number) =>
  request<void>({ method: 'POST', url: `/orders/${id}/cancel` })

export const grabOrder = (id: number, technicianId?: number) =>
  request<void>({ method: 'POST', url: `/orders/${id}/grab`, data: technicianId ? { technicianId } : {} })

export const acceptOrder = (id: number) =>
  request<void>({ method: 'POST', url: `/orders/${id}/accept` })

export const startOrder = (id: number) =>
  request<void>({ method: 'POST', url: `/orders/${id}/start` })

export const completeOrder = (id: number) =>
  request<void>({ method: 'POST', url: `/orders/${id}/complete` })

export const reviewOrder = (id: number, score: number, comment: string) =>
  request<void>({ method: 'POST', url: `/orders/${id}/review`, data: { score, comment } })

export const grabBoard = () => request<OrderView[]>({ method: 'GET', url: '/orders/grab-board' })

/* ---------- 商家 ---------- */
export const onboard = (data: { name: string; address?: string; lng: number; lat: number; radius?: number }) =>
  request<number>({ method: 'POST', url: '/merchant/onboard', data })

export const addTechnician = (data: { name: string; skill?: string; lng: number; lat: number }) =>
  request<number>({ method: 'POST', url: '/merchant/technicians', data })

export const createService = (data: {
  categoryId: number
  title: string
  description?: string
  price: number
  durationMin: number
  availableStart?: string
  availableEnd?: string
}) => request<number>({ method: 'POST', url: '/merchant/services', data })

export const myServices = () => request<ServiceView[]>({ method: 'GET', url: '/merchant/services' })

export const merchantProfile = () => request<MerchantView>({ method: 'GET', url: '/merchant/profile' })

/* ---------- 管理员 ---------- */
export const adminAudit = (id: number, approve: boolean, reason?: string) =>
  request<void>({ method: 'POST', url: `/admin/merchants/${id}/audit`, params: { approve, reason } })

export const adminMerchants = (status?: string) =>
  request<MerchantView[]>({ method: 'GET', url: '/admin/merchants', params: { status } })

export const createCategory = (name: string, parentId?: number, sort?: number) =>
  request<number>({ method: 'POST', url: '/admin/categories', params: { name, parentId, sort } })

export const adminOrders = (params?: { page?: number; size?: number; keyword?: string; status?: string }) =>
  request<PageResult<OrderView>>({ method: 'GET', url: '/admin/orders', params })

export const adminOrderDetail = (id: number) =>
  request<OrderView>({ method: 'GET', url: `/admin/orders/${id}` })

export const adminRefundApprove = (orderId: number) =>
  request<void>({ method: 'POST', url: `/admin/refunds/${orderId}/approve` })

export const adminRefundReject = (orderId: number) =>
  request<void>({ method: 'POST', url: `/admin/refunds/${orderId}/reject` })

export interface DashboardView {
  todayOrderCount: number
  todayGmv: number
  totalUsers: number
  totalMerchants: number
  pendingMerchants: number
  totalTechnicians: number
  pendingRefunds: number
  totalServices: number
}

export const adminDashboard = () => request<DashboardView>({ method: 'GET', url: '/admin/dashboard' })

export interface UserView {
  id: number
  phone: string
  nickname: string
  status: number
  createdAt: string
}

export const adminUsers = (params?: { page?: number; size?: number; keyword?: string; status?: number }) =>
  request<PageResult<UserView>>({ method: 'GET', url: '/admin/users', params })

export const banUser = (id: number) => request<void>({ method: 'POST', url: `/admin/users/${id}/ban` })

export const unbanUser = (id: number) => request<void>({ method: 'POST', url: `/admin/users/${id}/unban` })

export const adminUserOrders = (id: number, params?: { page?: number; size?: number }) =>
  request<PageResult<OrderView>>({ method: 'GET', url: `/admin/users/${id}/orders`, params })

export interface TechnicianView {
  id: number
  name: string
  skill: string
  status: string
  rating: number
  merchantId: number | null
}

export const adminTechnicians = (params?: { page?: number; size?: number; keyword?: string; status?: string }) =>
  request<PageResult<TechnicianView>>({ method: 'GET', url: '/admin/technicians', params })

export const enableTechnician = (id: number) =>
  request<void>({ method: 'POST', url: `/admin/technicians/${id}/enable` })

export const disableTechnician = (id: number) =>
  request<void>({ method: 'POST', url: `/admin/technicians/${id}/disable` })

export interface ServiceView {
  id: number
  title: string
  price: number
  status: string
  merchantId: number
  categoryId: number
}

export const adminServices = (params?: { page?: number; size?: number; keyword?: string; status?: string }) =>
  request<PageResult<ServiceView>>({ method: 'GET', url: '/admin/services', params })

export const offlineService = (id: number) =>
  request<void>({ method: 'POST', url: `/admin/services/${id}/offline` })

export const restoreService = (id: number) =>
  request<void>({ method: 'POST', url: `/admin/services/${id}/restore` })

export interface AdminView {
  id: number
  phone: string
  nickname: string
  role: string
  status: number
}

export const adminAdmins = () => request<AdminView[]>({ method: 'GET', url: '/admin/admins' })

export const createAdmin = (data: { phone: string; password: string; nickname?: string }) =>
  request<AdminView>({ method: 'POST', url: '/admin/admins', data })
