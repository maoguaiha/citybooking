const api = require('../../utils/api')

Page({
  data: {
    mode: 'login', // login | register
    phone: '',
    password: '',
    nickname: '',
    roleIndex: 0,
    roles: [
      { value: 'CONSUMER', label: '消费者' },
      { value: 'MERCHANT', label: '商家' },
      { value: 'TECHNICIAN', label: '技师' }
    ],
    loading: false
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode })
  },
  onPhone(e) { this.setData({ phone: e.detail.value }) },
  onPassword(e) { this.setData({ password: e.detail.value }) },
  onNickname(e) { this.setData({ nickname: e.detail.value }) },
  onRole(e) { this.setData({ roleIndex: Number(e.detail.value) }) },

  saveSession(resp) {
    wx.setStorageSync('cb_token', resp.token)
    wx.setStorageSync('cb_user', { userId: resp.userId, role: resp.role })
  },

  async submit() {
    const { mode, phone, password, nickname, roleIndex, roles } = this.data
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!password || password.length < 6) {
      wx.showToast({ title: '密码至少 6 位', icon: 'none' })
      return
    }
    if (mode === 'register' && !nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const resp = mode === 'login'
        ? await api.login(phone, password)
        : await api.register(phone, password, nickname, roles[roleIndex].value)
      this.saveSession(resp)
      wx.showToast({ title: '成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 500)
    } catch (err) {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  wechatLogin() {
    wx.login({
      success: async (res) => {
        if (!res.code) {
          wx.showToast({ title: '微信登录失败', icon: 'none' })
          return
        }
        try {
          const resp = await api.wechatLogin(res.code)
          this.saveSession(resp)
          wx.showToast({ title: '登录成功', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 500)
        } catch (err) {
          wx.showToast({ title: err.message || '微信登录失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '微信登录失败', icon: 'none' })
    })
  }
})
