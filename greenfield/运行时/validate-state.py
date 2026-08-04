#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
validate-state.py — STATE.md 一致性校验器

检查 STATE.md 结构合法、phase_pointer 指向真实文件、node_status 状态合法、
current_node 不越界。Agent 可在长任务中途自检 STATE 是否被写花。

退出码: 0 = VALID, 1 = INVALID
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
VALID_STATUS = {"todo", "spec", "coding", "testing", "reviewing", "done"}


def main():
    p = os.path.join(ROOT, "STATE.md")
    if not os.path.exists(p):
        print("STATE: INVALID（缺失 STATE.md）")
        sys.exit(1)
    txt = open(p, encoding="utf-8").read()

    def get(key):
        m = re.search(r"^" + re.escape(key) + r":\s*(.*)$", txt, re.M)
        if not m:
            return None
        return m.group(1).split('#')[0].strip().strip('"').strip("'")

    errors = []
    branch = get("branch")
    if branch not in ("greenfield", "brownfield", ""):
        errors.append(f"branch 非法: {branch!r}（应为 greenfield/brownfield）")

    try:
        cn = int(get("current_node") or 0)
    except Exception:
        cn = None
        errors.append("current_node 不是合法整数")
    try:
        tn = int(get("total_nodes") or 0)
    except Exception:
        tn = None
        errors.append("total_nodes 不是合法整数")

    pp = get("phase_pointer")
    if pp and not os.path.exists(os.path.join(ROOT, pp)):
        errors.append(f"phase_pointer 指向不存在文件: {pp}")

    ns = {}
    in_block = False
    for line in txt.splitlines():
        if line.strip().startswith("node_status"):
            in_block = True
            continue
        if in_block:
            m = re.match(r"^\s*(\d+):\s*(\w+)", line)
            if m:
                ns[int(m.group(1))] = m.group(2)
            elif re.match(r"^[a-zA-Z_]+:", line):
                in_block = False
    for k, v in ns.items():
        if v not in VALID_STATUS:
            errors.append(f"node_status[{k}] 非法状态: {v!r}")

    if cn is not None and tn is not None and cn > tn + 1:
        errors.append(f"current_node({cn}) 超过 total_nodes+1({tn + 1})")

    if errors:
        print("STATE: INVALID")
        for e in errors:
            print("  -", e)
        sys.exit(1)
    print(f"STATE: VALID（branch={branch or '?'} node={cn}/{tn} phase={pp}）")
    sys.exit(0)


if __name__ == "__main__":
    main()
