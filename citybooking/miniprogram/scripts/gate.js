#!/usr/bin/env node
// 小程序自动化 gate：校验全部 .js 语法(node --check) 与全部 .json 可解析。
// 退出码 0 = 通过, 1 = 失败。供 STATE gate（PROJECT-FLOW.md 的 N10 节点）调用。
const { execFileSync } = require('child_process')
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..')
let fail = 0
const errors = []

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((d) => {
    const p = path.join(dir, d.name)
    if (d.isDirectory()) return walk(p)
    return [p]
  })
}

const files = walk(ROOT)
const js = files.filter((f) => f.endsWith('.js'))
const json = files.filter((f) => f.endsWith('.json'))

for (const f of js) {
  try {
    execFileSync(process.execPath, ['--check', f], { stdio: 'pipe' })
  } catch (e) {
    fail++
    errors.push('JS 语法错误: ' + path.relative(ROOT, f) + '\n' + (e.stderr ? e.stderr.toString() : e.message))
  }
}

for (const f of json) {
  try {
    JSON.parse(fs.readFileSync(f, 'utf8'))
  } catch (e) {
    fail++
    errors.push('JSON 解析错误: ' + path.relative(ROOT, f) + ' -> ' + e.message)
  }
}

console.log('小程序 gate：校验 ' + js.length + ' 个 JS 文件、' + json.length + ' 个 JSON 文件')
if (fail > 0) {
  console.log('FAIL (' + fail + ' 项)')
  errors.forEach((e) => console.log('  - ' + e))
  process.exit(1)
}
console.log('PASS')
process.exit(0)
