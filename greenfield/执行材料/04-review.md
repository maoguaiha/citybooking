# Phase 4 · 红线审查、验收确认与合并沉淀(greenfield / A6–A7)

> 本节点做十七维红线审查（A6）、验收确认、合并 + 沉淀（A7）。审查清单见 [../参考文件/redlines.md](../参考文件/redlines.md)；MR 模板见下文；Git 合并命令见 [../参考文件/git-usage.md](../参考文件/git-usage.md)。

## A6 红线审查（QA 一票否决）

- 按 [redlines.md §7.1–7.4](../参考文件/redlines.md) 十七维清单逐条核对；按改动类型跑 [§7.3 grep 三连](../参考文件/redlines.md#sec73)，证据贴 MR。
- **命中任意 🔴 红线 → 打回**：告知用户具体违反哪一条，提示 `[SYS: 审查不通过，退回 ../执行材料/03-coding.md 重新编码]`。

## A6.5 验收确认（合并前关卡）

- **复杂 / 核心节点**：必须由 **PM / 用户** 对照 Spec「验收标准」**逐条确认**通过，才允许合并；任一条不通过 → 退回 `../执行材料/03-coding.md` 改。
- **低风险节点**：agent 自检 + 冒烟通过即可，无需人工确认（但需在 MR 自查勾选）。

## A7 合并 + 沉淀

- feature 多次提交 `squash` 合并进 `main`（见 [git-usage.md](../参考文件/git-usage.md)）。
- 把「AI 在哪坑过我」案例写进作品集/复盘，**反哺 [spec-template.md 的 Spec「不改什么」](../参考文件/spec-template.md) 与 [占位符清单](../参考文件/spec-template.md#placeholders)**——越用越懂项目。

## MR 描述模板（agent 填充）

```markdown
## 需求
（贴 spec-<需求>.md 链接，见 02-spec.md）

## 改动点
（文件 + 行号 + 意图）

## 自查
- [ ] 风格约束已贴、产出为原住民代码
- [ ] 十七维清单 ①~⑰ 全绿（附 redlines.md §7.3 的 grep 证据）
- [ ] Spec 已按 §7.0 补必带项
- [ ] 节点测试门已通过（附单测/集成/验收结果）
- [ ] 验收已确认（复杂/核心节点需 PM/用户签字）

## 风险与回滚
（对应 Spec「不改什么」与「回滚」）

## AI 协作备注
（AI 哪个点想跑偏、被怎么拦住——面试弹药）
```

---

## 🏁 流程终止协议（节点级 + 项目级）

**【Agent 动作】**
0. **更新 `../运行时/STATE.md`**：`phase_pointer=../执行材料/04-review.md` / 记录 `last_sys`。
1. **红线违规** → **写回 STATE**：`phase_pointer=../执行材料/03-coding.md`；打回 `[SYS: 审查不通过，退回 ../执行材料/03-coding.md 重新编码]`，回到 `03-coding.md`。
2. **全绿 + 验收确认通过** →
   - 生成 MR/PR 描述（上模板）。
   - **若本节点为「核心」风险（PROJECT-FLOW 标注）** → **写回 STATE**：`phase_pointer=../执行材料/05-deploy.md`；输出 `[SYS: 阶段 04 结束，已合并，进入 05-上线验证]`，读取 `../执行材料/05-deploy.md` 做上线验证。
   - **若为普通节点** → 标记 `../运行时/PROJECT-FLOW.md` 本节点 `done`，`../运行时/STATE.md` `current_node +1` 且 `total_nodes` 刷新为 `../运行时/PROJECT-FLOW.md` 当前节点总数；**写回 STATE**：`phase_pointer=../执行材料/02-spec.md`（还有节点则下次循环进入下一节点 / 无节点则下次循环命中终止符）；还有节点输出 `[SYS: 节点N通过，流转至节点N+1]`，无节点输出 `[SYS: 全部节点通过，流水线执行完毕。]` 待机。
