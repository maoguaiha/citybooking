const { BASE_URL } = require('./config')

// 统一请求封装：自动附带 JWT，处理 {code,message,data} 契约、401/403 跳登录。
function request(method, url, data) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('cb_token') || ''
    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data || {},
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        const body = res.data
        if (res.statusCode === 401 || res.statusCode === 403) {
          wx.removeStorageSync('cb_token')
          wx.removeStorageSync('cb_user')
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error((body && body.message) || '登录已失效，请重新登录'))
          return
        }
        if (body && body.code === 0) {
          resolve(body.data)
        } else {
          reject(new Error((body && body.message) || '请求失败'))
        }
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || '网络异常'))
      }
    })
  })
}

module.exports = {
  get: (url, data) => request('GET', url, data),
  post: (url, data) => request('POST', url, data)
}
