// CityBooking 小程序自动化冒烟测试（Windows / miniprogram-automator 0.12.1）
//
// 关键约束（已实测验证）：
//   - mp.* 操作可用：callWxMethod / setStorageSync / getStorageSync / switchTab / navigateTo /
//     reLaunch / currentPage。
//   - page.* 操作（data / setData / callMethod / evaluate）在本 SDK 版本会挂死，故一律不使用。
// 因此本测试通过「导航是否成功 + 对应页面消费的接口是否返回正确数据」来验证核心流程：
//   登录页 -> 登录(接口) -> 首页(服务列表接口) -> 服务详情(接口) -> 我的订单(接口)。
//
// 用法： node smoke.js [--loop N]
// 循环友好：结束时关闭 ws 连接但保留 cli 进程，使 9420 端口可复用；单连接约束下用 --loop 在同一次
//          连接内重复全流程。

const { ensureIDE } = require('./ide')
const fs = require('fs')
const path = require('path')

const LOGPATH = path.join(__dirname, 'smoke_run.log')
fs.writeFileSync(LOGPATH, '')
console.log = (...a) => fs.appendFileSync(LOGPATH, a.map(x => (x && x.stack) || String(x)).join(' ') + '\n')
console.error = (...a) => fs.appendFileSync(LOGPATH, 'ERR ' + a.map(x => (x && x.stack) || String(x)).join(' ') + '\n')

const BASE = 'http://127.0.0.1:18100/api'
const ADMIN = { phone: '10000000000', password: 'Admin@123456' }
const sleep = ms => new Promise(r => setTimeout(r, ms))
const withTimeout = (p, ms, l) => Promise.race([p, new Promise((_, r) => setTimeout(() => r(new Error(l + ' 超时(' + ms + 'ms)')), ms))])

const results = []
function check(name, cond, detail) {
  const pass = !!cond
  results.push({ name, pass, detail: detail || '' })
  console.log((pass ? 'PASS ' : 'FAIL ') + name + (detail ? '  -> ' + detail : ''))
}

// 在模拟器上下文发起 wx.request（带鉴权头），返回解析后的 res.data
async function req(mp, method, url, data, token) {
  const opt = { url: BASE + url, method }
  if (data) opt.data = data
  if (token) opt.header = { Authorization: 'Bearer ' + token }
  const r = await mp.callWxMethod('request', opt)
  return r && r.data
}

async function runOnce(mp, idx) {
  console.log('\n===== 第 ' + idx + ' 轮 =====')
  const once = []

  // 回到登录页（循环场景下复用连接）
  await mp.reLaunch('/pages/login/login')
  await sleep(1500)
  let page = await mp.currentPage()
  check('初始页面为登录页', page && /login/.test(page.path), 'path=' + (page && page.path))
  once.push(results[results.length - 1])

  // 登录（模拟器上下文 wx.request -> 后端）
  const login = await withTimeout(req(mp, 'POST', '/auth/login', { phone: ADMIN.phone, password: ADMIN.password }), 15000, 'login')
  const token = login && login.data && login.data.token
  check('登录接口返回 token', login && login.code === 0 && !!token, 'code=' + (login && login.code) + ' role=' + (login && login.data && login.data.role))
  once.push(results[results.length - 1])

  // 写入会话存储，使真实页面可正常鉴权
  await mp.callWxMethod('setStorageSync', 'cb_token', token)
  await mp.callWxMethod('setStorageSync', 'cb_user', { userId: login.data.userId, role: login.data.role })

  // 进入首页
  await mp.switchTab('/pages/home/home')
  await sleep(3000)
  page = await mp.currentPage()
  check('跳转到首页', page && /home/.test(page.path), 'path=' + (page && page.path))
  once.push(results[results.length - 1])

  // 首页服务列表（对应 api.searchServices -> GET /services，返回 {code,data:{records,total}}）
  const board = await withTimeout(req(mp, 'GET', '/services', { page: 1, size: 10 }, token), 15000, 'services')
  const records = (board && board.data && board.data.records) || []
  check('首页服务列表接口返回数据', board && board.code === 0 && records.length > 0, '服务数=' + records.length)
  once.push(results[results.length - 1])

  // 服务详情（对应 api.serviceDetail -> GET /services/{id}，返回 {code,data:{...}}）
  if (records.length) {
    const id = records[0].id
    // 注：本 SDK 版本 navigateTo 的 promise 偶发 spurious reject（Uncaught [object Object]），
    // 但页面通常已完成跳转；故不依赖其返回值，改为事后用 currentPage 核验真实落地页。
    try { await withTimeout(mp.navigateTo('/pages/service-detail/service-detail?id=' + id), 8000, 'navigateTo detail').catch(() => {}) } catch (e) {}
    await sleep(3000)
    page = await mp.currentPage()
    check('进入服务详情页', page && /service-detail/.test(page.path), 'path=' + (page && page.path))
    once.push(results[results.length - 1])
    try {
      const detail = await withTimeout(req(mp, 'GET', '/services/' + id, null, token), 15000, 'serviceDetail')
      check('服务详情接口返回数据', detail && detail.code === 0 && detail.data && (detail.data.title || detail.data.price != null),
        'title=' + (detail && detail.data && detail.data.title))
    } catch (e) {
      check('服务详情接口返回数据', false, '请求异常: ' + JSON.stringify(e && e.message || e))
    }
    once.push(results[results.length - 1])
  }

  // 我的订单（对应 api.myOrders -> GET /orders，返回 {code,data:{records,total}}）
  try {
    await withTimeout(mp.switchTab('/pages/orders/orders'), 15000, 'switchTab orders')
    await sleep(3000)
    page = await mp.currentPage()
    check('进入我的订单页', page && /orders/.test(page.path), 'path=' + (page && page.path))
  } catch (e) {
    check('进入我的订单页', false, 'switchTab 异常: ' + JSON.stringify(e && e.message || e))
  }
  once.push(results[results.length - 1])
  try {
    const orders = await withTimeout(req(mp, 'GET', '/orders', { status: '', page: 1, size: 50 }, token), 15000, 'myOrders')
    check('订单接口可用(数组)', orders && orders.code === 0 && Array.isArray((orders.data && orders.data.records) || orders.data),
      '订单数=' + (((orders && orders.data && orders.data.records) || []).length))
  } catch (e) {
    check('订单接口可用(数组)', false, '请求异常: ' + JSON.stringify(e && e.message || e))
  }
  once.push(results[results.length - 1])

  return once
}

;(async () => {
  const watchdog = setTimeout(() => { console.error('WATCHDOG: 强制退出（疑似卡死）'); process.exit(2) }, 150000)
  watchdog.unref()
  let miniProgram = null
  try {
    const loop = Math.max(1, parseInt(process.argv.find(a => /^\d+$/.test(a)) || '1', 10))
    miniProgram = await ensureIDE()
    console.log('==> Connected to IDE automation.')
    for (let i = 1; i <= loop; i++) {
      await runOnce(miniProgram, i)
    }
    const passed = results.filter(r => r.pass).length
    console.log('\n==== SUMMARY: ' + passed + '/' + results.length + ' checks passed ====')
  } catch (e) {
    console.error('TEST ERROR: ' + (e && e.stack || e))
  } finally {
    if (miniProgram) { try { await miniProgram.close() } catch (e) {} }
    const failed = results.filter(r => !r.pass).length
    process.exit(failed > 0 ? 1 : 0)
  }
})()
