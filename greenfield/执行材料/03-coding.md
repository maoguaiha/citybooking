# Phase 3 · 编码实现与 Git 规范(greenfield / A4–A5)

> 本节点编码并贴风格约束（A4）、单测焊红线（A5）、跑节点测试门。Git 规范见 [../参考文件/git-usage.md](../参考文件/git-usage.md)；红线清单见 [../参考文件/redlines.md](../参考文件/redlines.md)。

## A4 编码 + 贴风格约束（FE/BE + AI）★代码生成

- **agent 动作**：先**读框架/项目的 3–5 个源文件** derive 风格约束（命名、目录分层、持久化套路、返回结构、错误处理、注入方式），再粘进 Prompt。
- 产出代码 + 提交信息草稿（写明「动了哪几跳 / 不改什么」，提交规范见 [git-usage.md](../参考文件/git-usage.md)）。
- 高并发链路：原子性/扣减在真相源处完成（Redis Lua / DB 事务 / 第三方幂等），**跨系统一致性不靠单库 `@Transactional` 兜底**（红线见 [redlines.md §7.1②](../参考文件/redlines.md#sec71) / [§7.3⑬](../参考文件/redlines.md#sec73)）。
- **铁律**：动手前先让 AI 复述「改哪几跳、不动哪几跳」，复述不对不许动。

## A5 单测焊红线（BE + AI）★测试生成

- 给改动的业务方法补单测，尤其用 `assertThrows` 把红线（如「异常必须冒泡」）焊死成会失败的测试。
- 命令：`{{测试运行命令}}`（占位符见 [spec-template.md §占位符](../参考文件/spec-template.md#placeholders)，Phase 1 已落地）。

## ★ 节点测试门（Phase 3 末尾关卡）

编码 + 单测完成后，按 `../运行时/PROJECT-FLOW.md` 本节点的「测试门」定义判定：

| 节点类型 | 测试门内容 | 通过标准（要可运行，不要"手动"） |
| --- | --- | --- |
| 普通节点 | 单测全绿 + 该节点接口/流程冒烟 | 单测 0 失败；核心路径冒烟命令/curl/端到端脚本绿 |
| 复杂 / 核心节点 | 上式 + **集成测试 + 验收测试** | 跨模块/跨服务链路通；[Spec 验收标准](../参考文件/spec-template.md)逐条核对通过 |

- **fail** → 输出 `[SYS: 节点N测试未过，停留 ../执行材料/03-coding.md 修改]`，回 A4 改，不前进。
- **pass** → 进入 `../执行材料/04-review.md`。

---

## 🔄 下一步流转协议

**【Agent 动作】**
代码编写完成、通过 `git add -p` 块级审收与 Commit、且节点测试门 pass 后：
0. **更新 `../运行时/STATE.md`**：`phase_pointer=../执行材料/03-coding.md`（保持）/ `node_status[当前]=testing` / `last_sys`。
1. 停止编写任何业务代码。
2. **跑节点测试门**：`python ../运行时/gate.py --node <current_node>`
   - `GATE: FAIL` → 输出 `[SYS: 节点N测试未过，停留 ../执行材料/03-coding.md 修改]`，**写回 STATE**：`phase_pointer=../执行材料/03-coding.md`，**不前进**，回到本 Phase 修复。
   - `GATE: PASS` → **写回 STATE**：`phase_pointer=../执行材料/04-review.md`，立刻读取 `../执行材料/04-review.md`。
3. 输出：`[SYS: 阶段 03 结束，代码已暂存，进入 04-红线审查]`（仅 PASS 时）。
