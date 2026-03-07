#!/bin/bash

# Longport Platform - 自动化运行脚本
# 循环运行 Claude Code 完成任务

# 警告: 此脚本会自动运行多次 AI 任务，请确保有人监控

set -e

# 默认运行次数
RUNS=${1:-1}

echo "=========================================="
echo "  Longport Platform - 自动化运行脚本"
echo "=========================================="
echo ""
echo "警告: 此脚本将自动运行 Claude Code $RUNS 次"
echo "请确保有人监控 AI 的操作"
echo ""
read -p "确认继续？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

# 循环运行
for i in $(seq 1 $RUNS); do
    echo ""
    echo "=========================================="
    echo "第 $i/$RUNS 次运行"
    echo "=========================================="
    echo ""

    # 读取当前任务
    CURRENT_TASK=$(cat task.json | grep -o '"id": "[^"]*"' | grep -A1 'false' | head -1 | cut -d'"' -f4)

    if [ -z "$CURRENT_TASK" ]; then
        echo "所有任务已完成！"
        exit 0
    fi

    echo "当前任务: $CURRENT_TASK"
    echo ""

    # 运行 Claude Code
    # 使用 dangerously-skip-permissions 模式
    claude -p --dangerously-skip-permissions "请读取 task.json，完成下一个未完成的任务。完成后更新 task.json 和 progress.txt，并提交代码。"

    echo ""
    echo "第 $i 次运行完成"
    echo ""

    # 如果不是最后一次，等待一会
    if [ $i -lt $RUNS ]; then
        echo "等待 5 秒后继续..."
        sleep 5
    fi
done

echo ""
echo "=========================================="
echo "自动化运行完成！共运行 $RUNS 次"
echo "=========================================="
