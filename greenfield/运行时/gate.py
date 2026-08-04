#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gate.py — 节点测试门机械执行器（流水线强制校验）

读取 STATE.md 拿到当前节点，从 PROJECT-FLOW.md 的「机器可读」JSON 块读取该节点
的测试命令，真实执行并返回退出码。Agent 只能以本脚本的退出码判定测试门是否通过，
禁止凭记忆声称"通过了"。

用法:
  python gate.py            # 用 STATE.md 的 current_node
  python gate.py --node 2   # 指定节点（0 基，N0=0）
  python gate.py --all      # 依次跑全部节点
  python gate.py --dry      # 只打印将要执行的命令，不执行

退出码: 0 = PASS, 1 = FAIL, 2 = 配置/解析错误
"""
import argparse
import json
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))


def read_state():
    p = os.path.join(ROOT, "STATE.md")
    if not os.path.exists(p):
        return None
    txt = open(p, encoding="utf-8").read()
    data = {}
    for line in txt.splitlines():
        m = re.match(r"^([a-zA-Z_]+):\s*(.*)$", line)
        if m:
            data[m.group(1)] = m.group(2).split('#')[0].strip().strip('"').strip("'")
    # node_status 子块
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
    data["node_status"] = ns
    return data


def parse_tests():
    p = os.path.join(ROOT, "PROJECT-FLOW.md")
    if not os.path.exists(p):
        return {}
    txt = open(p, encoding="utf-8").read()
    m = re.search(r"```machine\s*\n(.*?)```", txt, re.S)
    if not m:
        return {}
    block = m.group(1)
    try:
        data = json.loads(block)
    except Exception as e:  # noqa: BLE001
        print("GATE: ERROR 机器可读块 JSON 解析失败:", e)
        return {}
    tests = {}
    for node in data.get("nodes", []):
        nid = node.get("id", "")
        num = None
        if isinstance(nid, str) and nid.startswith("N"):
            try:
                num = int(nid[1:])
            except ValueError:
                continue
        elif isinstance(nid, int):
            num = nid
        if num is None:
            continue
        tests[num] = node
    return tests


def run_node(node, spec):
    print(f"GATE: node={node} name={spec.get('name', '?')} status={spec.get('status', '?')}")
    cmds = []
    if spec.get("test"):
        cmds.append(("test", spec["test"]))
    if spec.get("build"):
        cmds.append(("build", spec["build"]))
    if not cmds:
        print("  (无测试命令，跳过)")
        return 0
    failed = []
    for name, c in cmds:
        print(f"--- {name}: {c}")
        r = subprocess.run(c, shell=True, cwd=os.path.dirname(ROOT))
        if r.returncode != 0:
            failed.append((name, c, r.returncode))
            print(f"    FAIL (exit {r.returncode})")
        else:
            print("    PASS")
    if failed:
        print(f"GATE: FAIL node={node}（{len(failed)} 项失败）")
        return 1
    print(f"GATE: PASS node={node}")
    return 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--node", type=int, default=None)
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--dry", action="store_true")
    args = ap.parse_args()

    state = read_state()
    if state is None:
        print("GATE: ERROR 找不到 STATE.md（请先执行 01-init 初始化）")
        sys.exit(2)
    tests = parse_tests()
    if not tests:
        print("GATE: ERROR PROJECT-FLOW.md 中无机器可读测试定义（```machine 块）")
        sys.exit(2)

    if args.all:
        nodes = sorted(tests.keys())
    else:
        node = args.node if args.node is not None else int(state.get("current_node", 0) or 0)
        if node not in tests:
            print(f"GATE: ERROR 无节点 {node} 的测试定义")
            sys.exit(2)
        nodes = [node]

    if args.dry:
        for n in nodes:
            spec = tests[n]
            print(f"  [dry] node={n} name={spec.get('name')} "
                  f"test={spec.get('test')} build={spec.get('build')}")
        sys.exit(0)

    overall = 0
    for n in nodes:
        rc = run_node(n, tests[n])
        overall = overall or rc
    sys.exit(overall)


if __name__ == "__main__":
    main()
