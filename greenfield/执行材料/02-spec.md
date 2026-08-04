# Phase 2 · 需求拆解与架构约束(greenfield / A1–A3)

> 本节点写 PRD / 定级 / Spec 五段。角色分工见 [../参考文件/roles.md](../参考文件/roles.md)；Spec 模板与必带项见 [../参考文件/spec-template.md](../参考文件/spec-template.md)。

## A1 写产品需求文档 PRD（PM/TL）★文档生成

- 从选定框架的**已有能力**反推功能范围：哪些直接复用、哪些要改、哪些要新写。
- PRD 五段（agent 填充，模板见 [spec-template.md](../参考文件/spec-template.md)）：背景目标 / 框架底座 / 功能范围(MVP→后续) / 非目标 / 验收标准。

## A2 定级（PM/TL）

- 看改动是否触碰「钱 / 货 / 资产 / 幂等 / 锁 / 消息 / 外部调用」。
- 🔴 高风险 → 走完整 A3–[A7](04-review.md)；🟡 低风险（样式 / 加字段 / 文案）→ 精简：Spec「范围+不改什么」+ 风格约束 + 审查 [⑥](../参考文件/redlines.md#sec72)。

## A3 写 Spec 五段（PM/TL）★文档生成

- 用 [spec-template.md §Spec 五段](../参考文件/spec-template.md) 模板（背景目标 / 范围 / 不改什么 / 验收 / 回滚）。
- **按改动类型补必带项**：见 [spec-template.md §7.0](../参考文件/spec-template.md#sec70)（并发/内存/池化/Spring 容器等），AI 才知道边界。
- **铁律**：动手前先让 AI 复述「改哪几跳、不动哪几跳」（见 `../执行材料/03-coding.md` A4），复述不对不许动。
- 保存为 `spec-<需求>.md`，并在 `../运行时/PROJECT-FLOW.md` 对应节点登记「Spec 文件」与「测试门」。

---

## 🔄 下一步流转协议

**【Agent 动作】**
完成 Spec 五段、用户（PM/TL）确认通过后：
0. **更新 `../运行时/STATE.md`**：`phase_pointer=../执行材料/02-spec.md` / `node_status[当前]=coding` / `total_nodes=N`（N=`../运行时/PROJECT-FLOW.md` 当前节点总数，按节点表行数计，每轮刷新以覆盖新增节点）/ `last_sys`。
1. 保存 `spec-<需求>.md`，更新 `../运行时/PROJECT-FLOW.md` 该行状态。
2. **写回 `../运行时/STATE.md`**：`phase_pointer=../执行材料/03-coding.md`（正式流转至下一 Phase）。
3. 立刻读取：`../执行材料/03-coding.md`。
4. 输出：`[SYS: 阶段 02 结束，契约已锁定，已自动流转至 03-编码执行]`。
