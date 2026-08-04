App({
  globalData: {
    location: null
  },
  onLaunch() {
    const loc = wx.getStorageSync('cb_loc')
    if (loc) this.globalData.location = loc
  }
})
