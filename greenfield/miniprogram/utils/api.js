// 与后端 /api 契约一致（见 web/src/lib/api.ts）。
const http = require('./request')

const api = {
  /* 鉴权 */
  register: (phone, password, nickname, role) =>
    http.post('/auth/register', { phone, password, nickname, role }),
  login: (phone, password) => http.post('/auth/login', { phone, password }),
  wechatLogin: (code) => http.post('/auth/wechat-login', { code }),
  me: () => http.get('/auth/me'),

  /* 服务 / LBS */
  searchServices: (params) => http.get('/services', params || {}),
  serviceDetail: (id) => http.get('/services/' + id),
  publicCategories: () => http.get('/services/categories'),

  /* 订单 */
  createOrder: (data) => http.post('/orders', data),
  payOrder: (id) => http.post('/orders/' + id + '/pay'),
  myOrders: (status) => http.get('/orders', { status: status || '', page: 1, size: 50 }),
  orderDetail: (id) => http.get('/orders/' + id),
  cancelOrder: (id) => http.post('/orders/' + id + '/cancel'),
  reviewOrder: (id, score, comment) => http.post('/orders/' + id + '/review', { score, comment }),

  /* 商家/技师 履约 */
  grabBoard: () => http.get('/orders/grab-board'),
  grabOrder: (id) => http.post('/orders/' + id + '/grab'),
  acceptOrder: (id) => http.post('/orders/' + id + '/accept'),
  startOrder: (id) => http.post('/orders/' + id + '/start'),
  completeOrder: (id) => http.post('/orders/' + id + '/complete')
}

module.exports = api
