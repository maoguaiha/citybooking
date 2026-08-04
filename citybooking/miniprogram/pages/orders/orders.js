const api = require('../../utils/api')
const { money, statusText, fmtTime } = require('../../utils/format')

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'unpaid', label: '待支付' },
  { key: 'active', label: '进行中' },
  { key: 'done', label: '已完成' }
]

const FILTERS = {
  all: () => true,
  unpaid: (o) => o.status === 'UNPAID',
  active: (o) => ['WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED', 'IN_SERVICE'].includes(o.status),
  done: (o) => ['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(o.status)
}

Page({
  data: {
    tabs: TABS,
    tab: 'all',
    all: [],
    list: [],
    loading: false
  },

  onShow() { this.load() },

  async load() {
    this.setData({ loading: true })
    try {
      const r = await api.myOrders('')
      const all = (r && r.records) || []
      all.forEach((o) => {
        o.amountText = money(o.amount)
        o.statusLabel = statusText(o.status)
        o.timeText = fmtTime(o.createdAt)
      })
      this.setData({ all })
      this.applyTab()
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
      wx.stopPullDownRefresh()
    }
  },

  onTab(e) {
    this.setData({ tab: e.currentTarget.dataset.key })
    this.applyTab()
  },

  applyTab() {
    const f = FILTERS[this.data.tab] || (() => true)
    this.setData({ list: this.data.all.filter(f) })
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id })
  },

  onPullDownRefresh() { this.load() }
})
