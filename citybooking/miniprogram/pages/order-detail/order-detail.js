const api = require('../../utils/api')
const { money, statusText, fmtTime } = require('../../utils/format')

Page({
  data: {
    id: null,
    order: null,
    isMerchant: false,
    canPay: false,
    canCancel: false,
    canReview: false,
    canAccept: false,
    canStart: false,
    canComplete: false,
    showReview: false,
    score: 5,
    comment: ''
  },

  onLoad(opt) {
    this.setData({ id: Number(opt.id) })
    this.load()
  },

  async load() {
    try {
      const o = await api.orderDetail(this.data.id)
      o.amountText = money(o.amount)
      o.statusLabel = statusText(o.status)
      o.timeText = fmtTime(o.createdAt)
      o.appointText = fmtTime(o.appointmentTime)
      const role = (wx.getStorageSync('cb_user') || {}).role
      const isMerchant = role === 'MERCHANT' || role === 'TECHNICIAN'
      this.setData({
        order: o,
        isMerchant,
        canPay: o.status === 'UNPAID',
        canCancel: ['UNPAID', 'WAIT_ACCEPT', 'PENDING_GRAB', 'ACCEPTED'].includes(o.status),
        canReview: !isMerchant && o.status === 'COMPLETED',
        canAccept: isMerchant && o.status === 'WAIT_ACCEPT',
        canStart: isMerchant && o.status === 'ACCEPTED',
        canComplete: isMerchant && o.status === 'SERVICING'
      })
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    }
  },

  async pay() {
    wx.showLoading({ title: '支付中' })
    try {
      await api.payOrder(this.data.id)
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '支付失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async cancel() {
    const r = await wx.showModal({ title: '取消订单', content: '确定要取消该订单吗？' })
    if (!r.confirm) return
    try {
      await api.cancelOrder(this.data.id)
      wx.showToast({ title: '已取消', icon: 'success' })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '取消失败', icon: 'none' })
    }
  },

  async accept() {
    try {
      await api.acceptOrder(this.data.id)
      wx.showToast({ title: '已接单', icon: 'success' })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '接单失败', icon: 'none' })
    }
  },
  async start() {
    try {
      await api.startOrder(this.data.id)
      wx.showToast({ title: '已开始服务', icon: 'success' })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  },
  async complete() {
    try {
      await api.completeOrder(this.data.id)
      wx.showToast({ title: '已完成', icon: 'success' })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  },

  openReview() { this.setData({ showReview: true }) },
  closeReview() { this.setData({ showReview: false }) },
  noop() {},
  onScore(e) { this.setData({ score: Number(e.currentTarget.dataset.s) }) },
  onComment(e) { this.setData({ comment: e.detail.value }) },

  async submitReview() {
    try {
      await api.reviewOrder(this.data.id, this.data.score, this.data.comment)
      wx.showToast({ title: '评价成功', icon: 'success' })
      this.setData({ showReview: false })
      this.load()
    } catch (e) {
      wx.showToast({ title: e.message || '评价失败', icon: 'none' })
    }
  }
})
