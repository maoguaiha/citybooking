const api = require('../../utils/api')
const { money, statusText, fmtTime } = require('../../utils/format')

Page({
  data: {
    isMerchant: false,
    // 消费者端
    keyword: '',
    categories: [],
    activeCat: 0,
    services: [],
    located: false,
    location: null,
    // 商家/技师端
    merchantTab: 'grab',
    grabList: [],
    mineList: [],
    loading: false
  },

  onShow() {
    const role = (wx.getStorageSync('cb_user') || {}).role
    const isMerchant = role === 'MERCHANT' || role === 'TECHNICIAN'
    if (this.data.isMerchant !== isMerchant) this.setData({ isMerchant })
    this.ensureLocation()
  },

  ensureLocation() {
    const loc = wx.getStorageSync('cb_loc')
    if (loc) {
      this.setData({ located: true, location: loc })
      this.load()
      return
    }
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const loc = { lng: res.longitude, lat: res.latitude }
        wx.setStorageSync('cb_loc', loc)
        this.setData({ located: true, location: loc })
        this.load()
      },
      fail: () => {
        this.setData({ located: false })
        this.load()
      }
    })
  },

  load() {
    if (this.data.isMerchant) this.loadMerchant()
    else this.loadConsumer()
  },

  /* ---------- 消费者端：浏览服务 ---------- */
  async loadConsumer() {
    this.setData({ loading: true })
    try {
      const params = {}
      if (this.data.located) {
        params.lng = this.data.location.lng
        params.lat = this.data.location.lat
      }
      if (this.data.activeCat) params.categoryId = this.data.activeCat
      if (this.data.keyword) params.keyword = this.data.keyword
      const r = await api.searchServices(params)
      const records = (r && r.records) || []
      records.forEach((s) => { s.priceText = money(s.price) })
      this.setData({ services: records })
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
      wx.stopPullDownRefresh()
    }
  },
  async loadCategories() {
    try { this.setData({ categories: await api.publicCategories() || [] }) } catch (e) {}
  },
  onSearch(e) { this.setData({ keyword: e.detail.value }) },
  onSearchConfirm() { if (!this.data.isMerchant) this.loadConsumer() },
  onCat(e) { this.setData({ activeCat: Number(e.currentTarget.dataset.id) }); this.loadConsumer() },
  reloc() { wx.removeStorageSync('cb_loc'); this.setData({ located: false }); this.ensureLocation() },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/service-detail/service-detail?id=' + e.currentTarget.dataset.id })
  },

  /* ---------- 商家/技师端：抢单大厅 + 我的接单 ---------- */
  async loadMerchant() {
    this.setData({ loading: true })
    try {
      if (this.data.merchantTab === 'grab') {
        const list = await api.grabBoard()
        ;(list || []).forEach((o) => {
          o.amountText = money(o.amount)
          o.statusLabel = statusText(o.status)
          o.timeText = fmtTime(o.appointmentTime)
        })
        this.setData({ grabList: list || [] })
      } else {
        const r = await api.myOrders('')
        const all = (r && r.records) || []
        const mine = all.filter((o) => ['WAIT_ACCEPT', 'ACCEPTED', 'SERVICING', 'COMPLETED'].includes(o.status))
        mine.forEach((o) => {
          o.amountText = money(o.amount)
          o.statusLabel = statusText(o.status)
          o.timeText = fmtTime(o.appointmentTime)
        })
        this.setData({ mineList: mine })
      }
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
      wx.stopPullDownRefresh()
    }
  },
  onMerchantTab(e) {
    this.setData({ merchantTab: e.currentTarget.dataset.tab })
    this.loadMerchant()
  },
  async grab(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '抢单中' })
    try {
      await api.grabOrder(id)
      wx.showToast({ title: '抢单成功', icon: 'success' })
      this.loadMerchant()
    } catch (err) {
      wx.showToast({ title: err.message || '抢单失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },
  goOrder(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id })
  },

  onPullDownRefresh() { this.load() }
})
