# 🤖 流水线自主驱动内核（Autonomous Driver · greenfield 分支）

> **用法**：把本分支文件夹（greenfield/）整体交给 Agent，并只给这一句初始提示词：
>
> 「读取 `执行材料/RUN.md`，你是本流水线的自主驱动内核。严格按照 `执行材料/RUN.md` 的『驱动循环』执行，直到输出终结符 `[SYS: 全部节点通过，流水线执行完毕]` 或命中 `blocked` 挂起。不要等待我逐步指示，自行推进每一个 Phase 与每一个节点。」
>
> 本文件让 Agent **自驱循环**整个流水线（A0 选型→A1~A3 Spec→A4~A5 编码/单测→节点测试门→A6~A7 审查/合并→[05 上线]→下一节点），无需人工在 Phase 之间接力。机械校验由 `../运行时/gate.py`（测试门）与 `../运行时/validate-state.py`（状态一致性）强制，Agent 无法用「我觉得过了」蒙混。

## 驱动循环（Agent 必须反复执行，除非 blocked 或全程 done）

> 每个 Phase 文件在「下一步流转协议 / 终止协议」里**已显式把 `phase_pointer` 写回为下一个 Phase 文件**。本循环只负责读取 `phase_pointer` 指向的文件并完整执行，无需自行推算下一 Phase。

1. **读 `../运行时/STATE.md`**
   - 不存在 → 先执行 `../执行材料/01-init.md`（A0 选型 + 占位符落地 + 用户拍板 + 写回 STATE），回到本循环。
   - `blocked:true` → **立即停止**，向用户报告 `block_reason`；待用户回复后清 `blocked`、从 `phase_pointer` 原样续跑（不重跑已完成 Phase）。
   - **`total_nodes` 为 0 / 空** → 用 `../运行时/PROJECT-FLOW.md` 节点表**当前真实行数**刷新（节点表为空则 `blocked:true`，提示用户先填节点表，停止等待）。
   - 正常 → 取 `current_node` / `total_nodes` / `phase_pointer`。
2. **`current_node > total_nodes`** → 输出 `[SYS: 全部节点通过，流水线执行完毕]`，停止。
3. **定位当前 Phase**：读取 `phase_pointer` 指向的文件，**完整执行其动作**（含该 Phase 自己在末尾跑 `gate.py` 测试门、并把 `phase_pointer` 写回为下一 Phase 文件）。
4. **测试门**：由 `../执行材料/03-coding.md` 在末尾执行 `python ../运行时/gate.py --node <current_node>`；`FAIL` → 停留 `../执行材料/03-coding.md` 不前进；`PASS` → 该 Phase 已把 `phase_pointer` 写回 `../执行材料/04-review.md` 并进入审查。
5. **红线审查**：由 `../执行材料/04-review.md` 执行 grep 三连 + 运行时三验证 + A6.5 验收确认，并自行写回 `phase_pointer`（违规退回 `../执行材料/03-coding.md` / 核心节点进 `../执行材料/05-deploy.md` / 普通节点进下一节点的 `../执行材料/02-spec.md`）。
6. **推进**：`phase_pointer` 已由上一 Phase 退出时写回为「下一 Phase 文件」，**直接读取并开始下一 Phase，不要停顿**。回到步骤 1。

## 铁律（违者流水线失效）
- 每个 Phase 流转前**必须**写回 `../运行时/STATE.md`（task_id / branch / current_node / phase_pointer / node_status / last_sys）。
- **测试门只能由 `../运行时/gate.py` 退出码判定**，禁止凭记忆声称通过。
- 命中「不确定就问」→ 立即 `blocked:true` 挂起，绝不擅自臆测。
- 除非 `blocked` 或 `done`，**不要停下等用户**，自主循环到终点。
- 长任务中途可用 `python ../运行时/validate-state.py` 自检 STATE 是否仍一致。
