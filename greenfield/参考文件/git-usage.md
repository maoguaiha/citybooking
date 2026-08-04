# 🔧 Git 规范 + 使用规范（两分支共用）

> 编码阶段（03-coding）与审查阶段（04-review）共用。命令细节见 [redlines.md](redlines.md)。

---

## Git 规范（agent 开发够用版）

```bash
git switch -c feature/<需求>      # 开分支隔离，main 不受 AI 实验牵连
git status -s && git diff         # 提交前必看 AI 改了啥
git add -p                        # 块级审收：逐块决定收不收，混进的无关改动直接跳过
git commit -m "type(模块): 一句话" # 写清信息 = 固化「动了哪几跳」
git push -u origin feature/<需求> # 推 + 开 PR（给自己一个复查停顿）
```

- **提交信息约定**：`type(模块): 描述`，type ∈ `feat/fix/refactor/test/docs/style/chore`。
- **改坏了三招救**：
  - `git restore <文件>` / `git restore --staged <文件>` —— 撤回未提交改动/暂存。
  - `git revert <commit>` —— 已合也可安全撤销（留痕，不删历史）。
  - `git reflog` —— 误 `reset` / 误删分支的救生圈。
- **临时换活儿**：`git stash` 存半成品 → 回来 `git stash pop`。
- **合并**：feature 多次提交 `squash` 压成一条干净记录进 `main`（见 04-review 合并步）。
- **.gitignore**：`{{构建产物/依赖/环境变量，如 target/ node_modules/ .env}}` 不提交；**勿误提交密钥与内部目录**。
- **禁止**：`git add -A` 一把抓；公共分支上 `git reset --hard` 改写历史。

---

## 使用规范 10 条

1. **先 Spec/PRD，再写码**：没有「不改什么」段不许动手。
2. **先复述，后动手**：让 AI 先说「改哪几跳、不动哪几跳」，复述不对打回。
3. **高风险必走全流**：涉及钱/货/资产/幂等/锁/消息的改动，红线一票否决。
4. **跨系统一致性不靠单库事务兜底**：真安全靠幂等 + 补偿/对账（Saga）。
5. **每个 feature 必开分支 + `git add -p` 块级审收**：AI 改动必须逐块过人眼。
6. **不提交未审查 / 未跑测试的改动**。
7. **AI 产出可审计**：commit message / MR 描述写清动线与红线自查。
8. **Spec 按改动类型补必带项**：写 Spec 时按 [spec-template.md §7.0](spec-template.md#sec70) 表格补必带项。
9. **收 AI 代码必跑 grep 三连**：按改动类型跑 [redlines.md §7.3](redlines.md#sec73) 对应的 grep 三连查坏 diff，证据贴 MR。
10. **不确定就问**（见 [roles.md §不确定就问](roles.md#ask)）：尤其技术选型、命根子判定、验收是否可测、部署实例范围、池参数/拒绝策略、JVM 基线。
