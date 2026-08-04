const api = require('../../utils/api')
const { money, fmtTime } = require('../../utils/format')

Page({
  data: {
    id: null,
    svc: null,
    mode: 'APPOINT', // APPOINT | GRAB
    address: '',
    date: '',
    time: '',
    loading: false
  },

  onLoad(opt) {
    this.setData({ id: Number(opt.id) })
    this.load()
  },

  async load() {
    try {
      const svc = await api.serviceDetail(this.data.id)
      svc.priceText = money(svc.price)
      // 无绑定商家时只能发抢单
      if (!svc.merchantId) this.setData({ mode: 'GRAB' })
      this.setData({ svc })
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    }
  },

  onMode(e) {
    const mode = e.currentTarget.dataset.mode
    if (mode === 'APPOINT' && !this.data.svc.merchantId) {
      wx.showToast({ title: '该服务暂不可指定商家', icon: 'none' })
      return
    }
    this.setData({ mode })
  },
  onAddress(e) { this.setData({ address: e.detail.value }) },
  onDate(e) { this.setData({ date: e.detail.value }) },
  onTime(e) { this.setData({ time: e.detail.value }) },

  async submit() {
    const { id, mode, address, date, time, svc } = this.data
    if (!address) {
      wx.showToast({ title: '请填写服务地址', icon: 'none' })
      return
    }
    const data = { serviceId: id, mode, address, appointmentTime: null }
    if (mode === 'APPOINT') data.merchantId = svc.merchantId
    if (date && time) data.appointmentTime = date + ' ' + time + ':00'

    this.setData({ loading: true })
    try {
      const oid = await api.createOrder(data)
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/pages/order-detail/order-detail?id=' + oid }), 500)
    } catch (e) {
      wx.showToast({ title: e.message || '下单失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
