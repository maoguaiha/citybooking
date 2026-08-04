# Phase 5 · 部署与上线验证(greenfield / 仅核心·高风险节点触发)

> 本节点在 `04-review` 全绿且节点为**核心/高风险**（PROJECT-FLOW 标「核心」）时触发；普通节点跳过本 Phase 直接 done。
> 解决「合进 main ≠ 上线」的真闭环缺口：把 [redlines.md ⑰](../参考文件/redlines.md#sec73) 的探针/Dockerfile 从「写代码要求」变成「部署后实测」。

## 动作（agent 执行，必要时拉运维/用户确认）

1. **构建**：多阶段镜像构建（`non-root`、配置外部化），见 [redlines.md ⑰](../参考文件/redlines.md#sec73)。
2. **部署**：发布到目标环境；确认 `Actuator health` 探针就绪（探真依赖，非假活）。
3. **灰度**：按比例灰度（canary），观察核心链路错误率/延迟/traceId 串联（采样 1.0）。
4. **线上验证**：跑核心链路冒烟（如 `curl`/端到端脚本），确认与 Phase 3 测试门同口径；核对日志 `traceId` 透传、无配置烧进镜像。
5. **回滚预案确认**：`kubectl rollout undo` / 开关 / 修复脚本随时可回退；记录回滚触发条件。

## 终止协议

**【Agent 动作】**
0. **更新 `../运行时/STATE.md`**：`phase_pointer=../执行材料/05-deploy.md`。
1. **上线验证失败**（探针不活 / 核心链路错 / 指标异常）→ 触发回滚，**写回 STATE**：`phase_pointer=../执行材料/03-coding.md`；提示 `[SYS: 上线验证失败，退回 ../执行材料/03-coding.md]`（回到编码排查根因）。
2. **验证通过** →
   - 标记 `../运行时/PROJECT-FLOW.md` 本节点 `done`，`../运行时/STATE.md` `current_node +1` 且 `total_nodes` 刷新为 `../运行时/PROJECT-FLOW.md` 当前节点总数。
   - **写回 STATE**：`phase_pointer=../执行材料/02-spec.md`（还有节点则下次循环进入下一节点 / 无节点则下次循环命中终止符）。
   - 若 **还有节点** → 输出 `[SYS: 节点N通过，流转至节点N+1]`。
   - 若 **已无节点** → 输出 `[SYS: 全部节点通过，流水线执行完毕。]` 随后待机。
