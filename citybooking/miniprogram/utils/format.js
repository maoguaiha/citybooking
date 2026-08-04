function money(n) {
  if (n == null) return '0.00'
  return Number(n).toFixed(2)
}

function statusText(s) {
  const map = {
    UNPAID: '待支付',
    WAIT_ACCEPT: '待接单',
    PENDING_GRAB: '抢单中',
    ACCEPTED: '已接单',
    SERVICING: '服务中',
    IN_SERVICE: '服务中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
    CLOSED: '已关闭'
  }
  return map[s] || s
}

// LocalDateTime 序列化为 "2026-08-04T10:00:00"，去掉中间的 T 并截断到秒。
function fmtTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}

module.exports = { money, statusText, fmtTime }
