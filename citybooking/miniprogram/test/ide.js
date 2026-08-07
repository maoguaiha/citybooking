// 共享：确保微信开发者工具自动化端口（9420）就绪并返回 automator 连接
// 幂等可循环：端口在监听则连接；被僵尸占用则 kill 后重连；否则两步启动。
const automator = require('miniprogram-automator')
const { spawn } = require('child_process')
const net = require('net')
const fs = require('fs')
const path = require('path')

const PROJECT = 'f:/program/document/同城预约服务平台/citybooking/miniprogram'
const CLI = 'G:/tool/develop/微信web开发者工具/cli.bat'
const PORT = 9420
const WS = 'ws://127.0.0.1:' + PORT
const childProcs = []

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

function portOpen(host, port) {
  return new Promise(res => {
    const s = net.connect(port, host)
    s.setTimeout(1500)
    s.on('connect', () => { s.destroy(); res(true) })
    s.on('error', () => { res(false) })
    s.on('timeout', () => { s.destroy(); res(false) })
  })
}

async function tryConnect(timeout) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    try { return await automator.connect({ wsEndpoint: WS }) }
    catch (e) { await sleep(800) }
  }
  return null
}

function killPort(port) {
  return new Promise((resolve) => {
    const cp = spawn('cmd', ['/c', 'netstat -ano | findstr :' + port], { windowsHide: true })
    let out = ''
    cp.stdout.on('data', d => { out += d.toString() })
    cp.on('close', () => {
      const pids = new Set()
      out.split('\n').forEach(line => {
        if (/LISTENING/.test(line)) {
          const m = line.trim().split(/\s+/)
          const pid = m[m.length - 1]
          if (/^\d+$/.test(pid)) pids.add(pid)
        }
      })
      if (!pids.size) return resolve()
      let remaining = pids.size
      pids.forEach(pid => {
        const k = spawn('cmd', ['/c', 'taskkill /F /PID ' + pid], { windowsHide: true })
        k.on('close', () => { if (--remaining === 0) resolve() })
      })
    })
  })
}

async function ensureIDE() {
  if (await portOpen('127.0.0.1', PORT)) {
    let mp = await tryConnect(8000)
    if (mp) return mp
    await killPort(PORT)
    await sleep(2000)
  }
  const yfile = path.join(__dirname, 'y.txt')
  fs.writeFileSync(yfile, 'y\n')
  const yfd = fs.openSync(yfile, 'r')
  console.log('==> step1: enable service port (feed y) ...')
  let c = spawn('cmd', ['/c', CLI, 'auto', '--project', PROJECT, '--auto-port', String(PORT), '--trust-project'],
    { stdio: [yfd, 'ignore', 'ignore'], windowsHide: false })
  childProcs.push(c)
  await sleep(12000)
  let mp = await tryConnect(5000)
  if (!mp) {
    console.log('==> step2: enter automation mode ...')
    c = spawn('cmd', ['/c', CLI, 'auto', '--project', PROJECT, '--auto-port', String(PORT), '--trust-project'],
      { stdio: ['ignore', 'ignore', 'ignore'], windowsHide: false })
    childProcs.push(c)
    mp = await tryConnect(60000)
  }
  if (!mp) throw new Error('IDE automation not ready')
  return mp
}

module.exports = { ensureIDE, PORT, WS, childProcs }
